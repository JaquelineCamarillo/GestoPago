package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.clientes.*;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.DomicilioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private DomicilioRepository domicilioRepository;
    @Mock
    private CuentaRepository cuentaRepository;

    private ClienteServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ClienteServiceImpl(clienteRepository, domicilioRepository, cuentaRepository);
    }

    private ClienteRequest requestValido() {
        ClienteRequest r = new ClienteRequest();
        r.setNombre("Juana");
        r.setApellidoPaterno("Camarillo");
        r.setApellidoMaterno("Olaez");
        r.setFechaNacimiento(LocalDate.now().minusYears(20));
        r.setCurp("CAOJ050101HGTMLN01");
        r.setRfc("CAOJ050101AB1");
        r.setSexo("M");
        r.setNacionalidad("Mexicana");
        r.setEstadoCivil("Soltera");
        r.setCorreoElectronico("juana@example.com");
        r.setTelefonoMovil("4771234567");
        r.setIngresoMensual(15000.0);
        r.setCalle("Reforma");
        r.setNumeroExterior("100");
        r.setColonia("Centro");
        r.setMunicipio("Leon");
        r.setEstado("Guanajuato");
        r.setCodigoPostal("37000");
        r.setPais("Mexico");
        return r;
    }

    @Test
    void creaCliente_datosValidos_creaClienteYCuentaAutomatica() {
        ClienteRequest request = requestValido();

        when(clienteRepository.findByCurp(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            c.setId(1);
            return c;
        });
        when(cuentaRepository.findByNumeroCuenta(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));

        ClienteResponse response = service.creaCliente(request);

        assertEquals("Juana", response.getNombre());
        assertNotNull(response.getNumeroCuenta());
        assertEquals("ACTIVA", response.getEstatusCuenta());
        assertEquals(0.0, response.getSaldo());
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
        verify(domicilioRepository, times(1)).save(any());
    }

    @Test
    void creaCliente_menorDeEdad_lanzaValidacionNegocioException() {
        ClienteRequest request = requestValido();
        request.setFechaNacimiento(LocalDate.now().minusYears(17));

        assertThrows(ValidacionNegocioException.class, () -> service.creaCliente(request));
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void creaCliente_curpDuplicada_lanzaCurpDuplicadaException() {
        ClienteRequest request = requestValido();
        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.of(new Cliente()));

        assertThrows(CurpDuplicadaException.class, () -> service.creaCliente(request));
    }

    @Test
    void creaCliente_rfcDuplicado_lanzaRfcDuplicadoException() {
        ClienteRequest request = requestValido();
        when(clienteRepository.findByCurp(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(request.getRfc())).thenReturn(Optional.of(new Cliente()));

        assertThrows(RfcDuplicadoException.class, () -> service.creaCliente(request));
    }

    @Test
    void creaCliente_correoDuplicado_lanzaCorreoDuplicadoException() {
        ClienteRequest request = requestValido();
        when(clienteRepository.findByCurp(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronico(request.getCorreoElectronico()))
                .thenReturn(Optional.of(new Cliente()));

        assertThrows(CorreoDuplicadoException.class, () -> service.creaCliente(request));
    }

    @Test
    void obtenerPorId_noExiste_lanzaClienteNoEncontradoException() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ClienteNoEncontradoException.class, () -> service.obtenerPorId(99));
    }

    @Test
    void bajaLogica_clienteExiste_desactivaClienteYCuenta() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setActivo(true);
        Cuenta cuenta = new Cuenta();
        cuenta.setEstatus("ACTIVA");

        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(cuentaRepository.findByClienteId(1)).thenReturn(Optional.of(cuenta));

        service.bajaLogica(1);

        assertFalse(cliente.getActivo());
        assertEquals("INACTIVA", cuenta.getEstatus());
        verify(clienteRepository, times(1)).save(cliente);
        verify(cuentaRepository, times(1)).save(cuenta);
    }
}