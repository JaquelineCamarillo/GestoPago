package com.proyecto.servicios.service;

import com.proyecto.servicios.client.ProveedorPagoClient;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Login;
import com.proyecto.servicios.entity.sf.Pago;
import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.enums.TipoPago;
import com.proyecto.servicios.exception.pagos.CodigoConfirmacionExpiradoException;
import com.proyecto.servicios.exception.pagos.CodigoConfirmacionInvalidoException;
import com.proyecto.servicios.exception.pagos.EstadoPagoInvalidoException;
import com.proyecto.servicios.exception.pagos.IntentosCodigoAgotadosException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.PagoDuplicadoException;
import com.proyecto.servicios.exception.pagos.ProductoCatalogoException;
import com.proyecto.servicios.exception.pagos.SaldoInsuficienteException;
import com.proyecto.servicios.exception.pagos.SesionInvalidaException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago.EstadoResultado;
import com.proyecto.servicios.model.pagos.ConfirmarPagoRequest;
import com.proyecto.servicios.model.pagos.ConsultarPagoRequest;
import com.proyecto.servicios.model.pagos.IniciarPagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.repositorys.sf.PagoBitacoraRepository;
import com.proyecto.servicios.repositorys.sf.PagoRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.service.Impl.PagoTransaccionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PagoServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private LoginRepository loginRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PagoBitacoraRepository bitacoraRepository;
    @Mock
    private GestoPagoProductService gestoPagoProductService;
    @Mock
    private ProveedorPagoClient proveedorPagoClient;
    @Mock
    private NotificadorCodigo notificadorCodigo;

    private TextEncryptor textEncryptor;
    private PagoTransaccionHelper transaccionHelper;
    private PagoServiceImpl pagoService;

    private Cliente clienteActivo;
    private Cuenta cuentaActiva;
    private Login loginActivo;
    private String tokenSesionValido;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        textEncryptor = Encryptors.text("clave-prueba-pagos", "a1b2c3d4e5f60718");
        transaccionHelper = new PagoTransaccionHelper(
                pagoRepository, bitacoraRepository, cuentaRepository,
                clienteRepository, loginRepository, textEncryptor
        );
        pagoService = new PagoServiceImpl(
                clienteRepository, cuentaRepository, pagoRepository,
                gestoPagoProductService, proveedorPagoClient,
                notificadorCodigo, textEncryptor, transaccionHelper, "083"
        );

        clienteActivo = new Cliente();
        clienteActivo.setId(1);
        clienteActivo.setNombre("Juana");
        clienteActivo.setCorreoElectronico("juana@example.com");
        clienteActivo.setActivo(true);

        cuentaActiva = new Cuenta();
        cuentaActiva.setId(10);
        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setNumeroCuenta("1234567890123456");
        cuentaActiva.setSaldo(500.00);
        cuentaActiva.setSaldoRetenido(new BigDecimal("0.00"));
        cuentaActiva.setEstatus("ACTIVA");

        tokenSesionValido = UUID.randomUUID().toString();
        loginActivo = new Login();
        loginActivo.setId(100);
        loginActivo.setCliente(clienteActivo);
        loginActivo.setActivo(true);
        loginActivo.setTokenCifrado(textEncryptor.encrypt(tokenSesionValido));
        loginActivo.setUltimaActividad(LocalDateTime.now());

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteActivo));
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.of(loginActivo));
        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.findByIdForUpdate(10)).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.findById(10)).thenReturn(Optional.of(cuentaActiva));
        when(gestoPagoProductService.listarProductosDisponibles(null)).thenReturn(List.of(
                new GestoPagoProductoResponse(200, 71, "Amazon", "Amazon $100", "100.0", 1),
                new GestoPagoProductoResponse(205, 76, "CFE", "CFE Recibo", "8.0", 2)
        ));
        when(pagoRepository.findByTrace(anyString())).thenReturn(Optional.empty());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(77);
            }
            return p;
        });
        when(bitacoraRepository.findByPagoIdOrderByFechaRegistroAscIdAsc(anyInt())).thenReturn(List.of());
        when(notificadorCodigo.enviarCodigoConfirmacion(any(), anyInt(), anyString()))
                .thenAnswer(inv -> inv.getArgument(2));
    }

    private IniciarPagoRequest requestRecargaValida() {
        IniciarPagoRequest r = new IniciarPagoRequest();
        r.setNumeroCuenta("1234567890123456");
        r.setIdUser(1);
        r.setTokenSesion(tokenSesionValido);
        r.setTipoPago(TipoPago.RECARGA);
        r.setIdServicio(71);
        r.setIdProd(200);
        r.setMonto(new BigDecimal("100.00"));
        r.setTelefono("5555555551");
        return r;
    }

    // ---------------- Validaciones de iniciarPago ----------------

    @Test
    void iniciarPago_clienteInactivo_lanzaOperacionNoAutorizada() {
        clienteActivo.setActivo(false);
        assertThrows(OperacionNoAutorizadaException.class,
                () -> pagoService.iniciarPago(requestRecargaValida()));
    }

    @Test
    void iniciarPago_sesionInvalidaOTokenIncorrecto_lanzaSesionInvalida() {
        IniciarPagoRequest req = requestRecargaValida();
        req.setTokenSesion(UUID.randomUUID().toString());
        assertThrows(SesionInvalidaException.class, () -> pagoService.iniciarPago(req));

        loginActivo.setUltimaActividad(LocalDateTime.now().minusMinutes(6));
        assertThrows(SesionInvalidaException.class,
                () -> pagoService.iniciarPago(requestRecargaValida()));
    }

    @Test
    void iniciarPago_cuentaAjenaOInactiva_lanzaOperacionNoAutorizada() {
        Cliente otro = new Cliente();
        otro.setId(99);
        cuentaActiva.setCliente(otro);

        assertThrows(OperacionNoAutorizadaException.class,
                () -> pagoService.iniciarPago(requestRecargaValida()));

        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setEstatus("INACTIVA");
        assertThrows(OperacionNoAutorizadaException.class,
                () -> pagoService.iniciarPago(requestRecargaValida()));
    }

    @Test
    void iniciarPago_productoInexistenteOMontoIncorrecto_lanzaProductoCatalogoException() {
        IniciarPagoRequest inexistente = requestRecargaValida();
        inexistente.setIdProd(9999);
        ProductoCatalogoException ex1 = assertThrows(ProductoCatalogoException.class,
                () -> pagoService.iniciarPago(inexistente));
        assertTrue(ex1.isNoEncontrado());

        IniciarPagoRequest montoDistinto = requestRecargaValida();
        montoDistinto.setMonto(new BigDecimal("150.00"));
        ProductoCatalogoException ex2 = assertThrows(ProductoCatalogoException.class,
                () -> pagoService.iniciarPago(montoDistinto));
        assertFalse(ex2.isNoEncontrado());
    }

    @Test
    void iniciarPago_saldoInsuficiente_lanzaSaldoInsuficienteException() {
        cuentaActiva.setSaldo(50.00);
        assertThrows(SaldoInsuficienteException.class,
                () -> pagoService.iniciarPago(requestRecargaValida()));
    }

    @Test
    void iniciarPago_solicitudDuplicadaEnVentana_lanzaPagoDuplicadoException() {
        when(pagoRepository.existeSolicitudDuplicadaEnVentana(
                anyInt(), anyInt(), anyInt(), any(), any(), any(), anyCollection(), any()))
                .thenReturn(true);

        assertThrows(PagoDuplicadoException.class,
                () -> pagoService.iniciarPago(requestRecargaValida()));
    }

    @Test
    void iniciarPago_exitoso_creaPagoEnPendienteConfirmacionSinMoverSaldo() {
        PagoResponse resp = pagoService.iniciarPago(requestRecargaValida());

        assertEquals(EstadoPago.PENDIENTE_CONFIRMACION, resp.getEstado());
        assertFalse(resp.getMontoRetenido());
        assertEquals(new BigDecimal("500.00"), resp.getSaldoDisponibleCuenta());
        assertNotNull(resp.getCodigoDemo());
        assertEquals(6, resp.getCodigoDemo().length());
        assertEquals(12, resp.getTrace().length());
        verifyNoInteractions(proveedorPagoClient);
    }

    // ---------------- Validaciones y flujo de confirmarPago ----------------

    private Pago crearPagoPendiente(String codigoPlano) {
        Pago p = new Pago();
        p.setId(77);
        p.setCliente(clienteActivo);
        p.setCuenta(cuentaActiva);
        p.setNumeroCuenta(cuentaActiva.getNumeroCuenta());
        p.setTipoPago(TipoPago.RECARGA);
        p.setIdServicio(71);
        p.setIdProducto(200);
        p.setNombreServicio("Amazon");
        p.setNombreProducto("Amazon $100");
        p.setMonto(new BigDecimal("100.00"));
        p.setTelefono("5555555551");
        p.setTrace("AB12CD34EF56");
        p.setHoraLocal("20261028190000");
        p.setUnidad("083");
        p.setEstado(EstadoPago.PENDIENTE_CONFIRMACION);
        p.setCodigoCifrado(textEncryptor.encrypt(codigoPlano));
        p.setCodigoExpiracion(LocalDateTime.now().plusMinutes(5));
        p.setIntentosCodigo(0);
        p.setMontoRetenido(false);
        return p;
    }

    @Test
    void confirmarPago_codigoIncorrectoTresVeces_agotaIntentosYRechazaPago() {
        Pago pago = crearPagoPendiente("123456");
        when(pagoRepository.findByIdForUpdate(77)).thenReturn(Optional.of(pago));

        ConfirmarPagoRequest req = new ConfirmarPagoRequest();
        req.setPagoId(77);
        req.setTokenSesion(tokenSesionValido);
        req.setCodigo("000000");

        assertThrows(CodigoConfirmacionInvalidoException.class, () -> pagoService.confirmarPago(req));
        assertEquals(1, pago.getIntentosCodigo());
        assertEquals(EstadoPago.PENDIENTE_CONFIRMACION, pago.getEstado());

        assertThrows(CodigoConfirmacionInvalidoException.class, () -> pagoService.confirmarPago(req));
        assertEquals(2, pago.getIntentosCodigo());

        assertThrows(IntentosCodigoAgotadosException.class, () -> pagoService.confirmarPago(req));
        assertEquals(3, pago.getIntentosCodigo());
        assertEquals(EstadoPago.RECHAZADO, pago.getEstado());
        verifyNoInteractions(proveedorPagoClient);
    }

    @Test
    void confirmarPago_codigoExpirado_marcaExpiradoYLanzaExcepcion() {
        Pago pago = crearPagoPendiente("123456");
        pago.setCodigoExpiracion(LocalDateTime.now().minusSeconds(5));
        when(pagoRepository.findByIdForUpdate(77)).thenReturn(Optional.of(pago));

        ConfirmarPagoRequest req = new ConfirmarPagoRequest();
        req.setPagoId(77);
        req.setTokenSesion(tokenSesionValido);
        req.setCodigo("123456");

        assertThrows(CodigoConfirmacionExpiradoException.class, () -> pagoService.confirmarPago(req));
        assertEquals(EstadoPago.EXPIRADO, pago.getEstado());
        verifyNoInteractions(proveedorPagoClient);
    }

    @Test
    void confirmarPago_dobleConfirmacion_lanzaEstadoPagoInvalido() {
        Pago pago = crearPagoPendiente("123456");
        pago.setEstado(EstadoPago.COMPLETADO);
        when(pagoRepository.findByIdForUpdate(77)).thenReturn(Optional.of(pago));

        ConfirmarPagoRequest req = new ConfirmarPagoRequest();
        req.setPagoId(77);
        req.setTokenSesion(tokenSesionValido);
        req.setCodigo("123456");

        assertThrows(EstadoPagoInvalidoException.class, () -> pagoService.confirmarPago(req));
        verifyNoInteractions(proveedorPagoClient);
    }

    @Test
    void confirmarPago_exitoProveedor_retieneConvierteEnCargoDefinitivoYMarcaCompletado() {
        Pago pago = crearPagoPendiente("654321");
        when(pagoRepository.findByIdForUpdate(77)).thenReturn(Optional.of(pago));
        when(proveedorPagoClient.enviarTransaccion(any())).thenReturn(
                ResultadoProveedorPago.builder()
                        .estado(EstadoResultado.EXITO)
                        .codigoProveedor("01")
                        .mensajeProveedor("Operacion realizada con exito")
                        .idTx("291437188")
                        .numAutorizacion("100024988")
                        .build()
        );

        ConfirmarPagoRequest req = new ConfirmarPagoRequest();
        req.setPagoId(77);
        req.setTokenSesion(tokenSesionValido);
        req.setCodigo("654321");

        PagoResponse resp = pagoService.confirmarPago(req);

        assertEquals(EstadoPago.COMPLETADO, resp.getEstado());
        assertFalse(resp.getMontoRetenido());
        assertEquals(new BigDecimal("400.00"), resp.getSaldoDisponibleCuenta());
        assertEquals(new BigDecimal("0.00"), resp.getSaldoRetenidoCuenta());
        assertEquals("291437188", resp.getIdTxProveedor());
        assertEquals("100024988", resp.getNumAutorizacion());
    }

    @Test
    void confirmarPago_rechazoDefinitivoProveedor_liberaRetencionReembolsaSaldoYMarcaFallido() {
        Pago pago = crearPagoPendiente("654321");
        when(pagoRepository.findByIdForUpdate(77)).thenReturn(Optional.of(pago));
        when(proveedorPagoClient.enviarTransaccion(any())).thenReturn(
                ResultadoProveedorPago.builder()
                        .estado(EstadoResultado.RECHAZO_DEFINITIVO)
                        .codigoProveedor("02")
                        .mensajeProveedor("Referencia invalida")
                        .numAutorizacion("-1")
                        .build()
        );

        ConfirmarPagoRequest req = new ConfirmarPagoRequest();
        req.setPagoId(77);
        req.setTokenSesion(tokenSesionValido);
        req.setCodigo("654321");

        PagoResponse resp = pagoService.confirmarPago(req);

        assertEquals(EstadoPago.FALLIDO, resp.getEstado());
        assertFalse(resp.getMontoRetenido());
        assertEquals(new BigDecimal("500.00"), resp.getSaldoDisponibleCuenta());
        assertEquals(new BigDecimal("0.00"), resp.getSaldoRetenidoCuenta());
    }

    @Test
    void confirmarPago_timeoutProveedor_mantieneRetencionYMarcaEnRevisionParaConciliar() {
        Pago pago = crearPagoPendiente("654321");
        when(pagoRepository.findByIdForUpdate(77)).thenReturn(Optional.of(pago));
        when(proveedorPagoClient.enviarTransaccion(any())).thenReturn(
                ResultadoProveedorPago.builder()
                        .estado(EstadoResultado.REQUIERE_CONCILIACION)
                        .codigoProveedor("82")
                        .mensajeProveedor("Tiempo de espera alcanzado")
                        .build()
        );

        ConfirmarPagoRequest req = new ConfirmarPagoRequest();
        req.setPagoId(77);
        req.setTokenSesion(tokenSesionValido);
        req.setCodigo("654321");

        PagoResponse resp = pagoService.confirmarPago(req);

        assertEquals(EstadoPago.EN_REVISION, resp.getEstado());
        assertTrue(resp.getMontoRetenido());
        assertEquals(new BigDecimal("400.00"), resp.getSaldoDisponibleCuenta());
        assertEquals(new BigDecimal("100.00"), resp.getSaldoRetenidoCuenta());

        // Conciliacion posterior con confirmTx.do -> CODIGO 06 (aplicada exitosamente)
        when(pagoRepository.findById(77)).thenReturn(Optional.of(pago));
        when(proveedorPagoClient.confirmarTransaccion(any())).thenReturn(
                ResultadoProveedorPago.builder()
                        .estado(EstadoResultado.EXITO)
                        .codigoProveedor("06")
                        .mensajeProveedor("La transaccion fue aplicada exitosamente")
                        .idTx("291437188")
                        .numAutorizacion("100024988")
                        .build()
        );

        ConsultarPagoRequest conciliarReq = new ConsultarPagoRequest();
        conciliarReq.setPagoId(77);
        conciliarReq.setTokenSesion(tokenSesionValido);

        PagoResponse conciliado = pagoService.conciliarPago(conciliarReq);
        assertEquals(EstadoPago.COMPLETADO, conciliado.getEstado());
        assertFalse(conciliado.getMontoRetenido());
        assertEquals(new BigDecimal("400.00"), conciliado.getSaldoDisponibleCuenta());
        assertEquals(new BigDecimal("0.00"), conciliado.getSaldoRetenidoCuenta());
    }
}
