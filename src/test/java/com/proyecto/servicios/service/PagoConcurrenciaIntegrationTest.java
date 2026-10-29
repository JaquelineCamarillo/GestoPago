package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.enums.TipoPago;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;
import com.proyecto.servicios.model.RegistrarCredencialesRequest;
import com.proyecto.servicios.model.pagos.ConfirmarPagoRequest;
import com.proyecto.servicios.model.pagos.ConsultarPagoRequest;
import com.proyecto.servicios.model.pagos.IniciarPagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.DomicilioRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.repositorys.sf.PagoBitacoraRepository;
import com.proyecto.servicios.repositorys.sf.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PagoConcurrenciaIntegrationTest {

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private CuentaService cuentaService;
    @Autowired
    private LoginService loginService;
    @Autowired
    private PagoService pagoService;

    @Autowired
    private GestoPagoProductoRepository productoRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PagoBitacoraRepository bitacoraRepository;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private LoginRepository loginRepository;
    @Autowired
    private DomicilioRepository domicilioRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    @MockBean
    private GestoPagoTokenService gestoPagoTokenService;

    private ClienteResponse cliente;
    private String tokenSesion;

    @BeforeEach
    void prepararDatos() {
        bitacoraRepository.deleteAll();
        pagoRepository.deleteAll();
        loginRepository.deleteAll();
        cuentaRepository.deleteAll();
        domicilioRepository.deleteAll();
        clienteRepository.deleteAll();
        productoRepository.deleteAll();

        GestoPagoProducto prodRecarga = new GestoPagoProducto();
        prodRecarga.setIdServicio(71);
        prodRecarga.setIdProducto(200);
        prodRecarga.setNombreServicio("Amazon");
        prodRecarga.setNombreProducto("Amazon $100");
        prodRecarga.setPrecio("100.0");
        prodRecarga.setTipoFront(1);
        prodRecarga.setActivo(true);
        productoRepository.save(prodRecarga);

        GestoPagoProducto prodServicio = new GestoPagoProducto();
        prodServicio.setIdServicio(76);
        prodServicio.setIdProducto(205);
        prodServicio.setNombreServicio("CFE");
        prodServicio.setNombreProducto("CFE Pago de Servicio");
        prodServicio.setPrecio("8.0");
        prodServicio.setTipoFront(2);
        prodServicio.setActivo(true);
        productoRepository.save(prodServicio);

        ClienteRequest req = new ClienteRequest();
        req.setNombre("Juana");
        req.setApellidoPaterno("Camarillo");
        req.setApellidoMaterno("Olaez");
        req.setFechaNacimiento(LocalDate.of(1995, 5, 18));
        req.setCurp("CAOJ950518MGTMLQ08");
        req.setRfc("CAOJ950518AB9");
        req.setSexo("M");
        req.setNacionalidad("MX");
        req.setEstadoCivil("Soltera");
        req.setCorreoElectronico("juana.pagos@example.com");
        req.setTelefonoMovil("4771234567");
        req.setIngresoMensual(25000.0);
        req.setCalle("Av Reforma");
        req.setNumeroExterior("120");
        req.setColonia("Centro");
        req.setMunicipio("Leon");
        req.setEstado("Guanajuato");
        req.setCodigoPostal("37000");
        req.setPais("MX");

        cliente = clienteService.creaCliente(req);
        cuentaService.abonarSaldo(cliente.getNumeroCuenta(), new BigDecimal("500.00"));

        RegistrarCredencialesRequest regLogin = new RegistrarCredencialesRequest();
        regLogin.setClienteId(cliente.getId());
        regLogin.setPassword("ClaveSegura#2026");
        loginService.registrarCredenciales(regLogin);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setCorreoElectronico("juana.pagos@example.com");
        loginReq.setPassword("ClaveSegura#2026");
        LoginResponse loginResp = loginService.iniciarSesion(loginReq);
        tokenSesion = loginResp.getToken();
    }

    @Test
    void dosConfirmacionesSimultaneasDelMismoPago_aplicanUnSoloCargoYMantienenSaldoConsistente() throws Exception {
        IniciarPagoRequest iniciar = new IniciarPagoRequest();
        iniciar.setNumeroCuenta(cliente.getNumeroCuenta());
        iniciar.setIdUser(cliente.getId());
        iniciar.setTokenSesion(tokenSesion);
        iniciar.setTipoPago(TipoPago.RECARGA);
        iniciar.setIdServicio(71);
        iniciar.setIdProd(200);
        iniciar.setMonto(new BigDecimal("100.00"));
        iniciar.setTelefono("5555555551");

        PagoResponse iniciado = pagoService.iniciarPago(iniciar);
        assertNotNull(iniciado.getCodigoDemo());

        int hilos = 2;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch disparo = new CountDownLatch(1);

        Callable<Boolean> tareaConfirmar = () -> {
            listos.countDown();
            disparo.await();
            ConfirmarPagoRequest conf = new ConfirmarPagoRequest();
            conf.setPagoId(iniciado.getPagoId());
            conf.setTokenSesion(tokenSesion);
            conf.setCodigo(iniciado.getCodigoDemo());
            try {
                PagoResponse r = pagoService.confirmarPago(conf);
                return r.getEstado() == EstadoPago.COMPLETADO;
            } catch (Exception e) {
                return false;
            }
        };

        List<Future<Boolean>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(tareaConfirmar));
        }

        listos.await();
        disparo.countDown();

        int exitosos = 0;
        for (Future<Boolean> f : futuros) {
            if (Boolean.TRUE.equals(f.get())) {
                exitosos++;
            }
        }
        pool.shutdown();

        // Exactamente UNA confirmacion debe procesarse; la segunda es rechazada por el bloqueo pesimista
        assertEquals(1, exitosos);

        Cuenta cuentaFinal = cuentaRepository.findByNumeroCuenta(cliente.getNumeroCuenta()).orElseThrow();
        assertEquals(400.00, cuentaFinal.getSaldo(), 0.001);
        assertEquals(new BigDecimal("0.00"), cuentaFinal.getSaldoRetenido());
    }

    @Test
    void flujoTimeoutYConciliacion_mantieneRetencionYLaResuelveCorrectamente() {
        IniciarPagoRequest iniciar = new IniciarPagoRequest();
        iniciar.setNumeroCuenta(cliente.getNumeroCuenta());
        iniciar.setIdUser(cliente.getId());
        iniciar.setTokenSesion(tokenSesion);
        iniciar.setTipoPago(TipoPago.SERVICIO);
        iniciar.setIdServicio(76);
        iniciar.setIdProd(205);
        iniciar.setMonto(new BigDecimal("150.00"));
        iniciar.setReferencia("TIMEOUT12345");

        PagoResponse iniciado = pagoService.iniciarPago(iniciar);

        ConfirmarPagoRequest conf = new ConfirmarPagoRequest();
        conf.setPagoId(iniciado.getPagoId());
        conf.setTokenSesion(tokenSesion);
        conf.setCodigo(iniciado.getCodigoDemo());

        PagoResponse enRevision = pagoService.confirmarPago(conf);
        assertEquals(EstadoPago.EN_REVISION, enRevision.getEstado());
        assertTrue(enRevision.getMontoRetenido());
        assertEquals(new BigDecimal("350.00"), enRevision.getSaldoDisponibleCuenta());
        assertEquals(new BigDecimal("150.00"), enRevision.getSaldoRetenidoCuenta());

        ConsultarPagoRequest conciliar = new ConsultarPagoRequest();
        conciliar.setPagoId(iniciado.getPagoId());
        conciliar.setTokenSesion(tokenSesion);

        PagoResponse conciliado = pagoService.conciliarPago(conciliar);
        assertEquals(EstadoPago.COMPLETADO, conciliado.getEstado());
        assertFalse(conciliado.getMontoRetenido());
        assertEquals(new BigDecimal("350.00"), conciliado.getSaldoDisponibleCuenta());
        assertEquals(new BigDecimal("0.00"), conciliado.getSaldoRetenidoCuenta());
        assertFalse(conciliado.getBitacora().isEmpty());
    }
}
