package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Domicilio;
import com.proyecto.servicios.exception.clientes.*;
import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.DomicilioRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final int EDAD_MINIMA = 18;

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              DomicilioRepository domicilioRepository,
                              CuentaRepository cuentaRepository) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    @Transactional
    public ClienteResponse creaCliente(ClienteRequest request) {
        validarMayoriaDeEdad(request.getFechaNacimiento());
        validarUnicidad(request);

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);
        cliente = clienteRepository.save(cliente);

        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        domicilio.setCalle(request.getCalle());
        domicilio.setNumeroExterior(request.getNumeroExterior());
        domicilio.setNumeroInterior(request.getNumeroInterior());
        domicilio.setColonia(request.getColonia());
        domicilio.setMunicipio(request.getMunicipio());
        domicilio.setEstado(request.getEstado());
        domicilio.setCodigoPostal(request.getCodigoPostal());
        domicilio.setPais(request.getPais());
        domicilioRepository.save(domicilio);

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(0.0);
        cuenta.setEstatus("ACTIVA");
        cuenta = cuentaRepository.save(cuenta);

        log.info("Cliente creado id={}, cuenta={}", cliente.getId(), cuenta.getNumeroCuenta());
        return aResponse(cliente, cuenta);
    }

    @Override
    public ClienteResponse obtenerPorId(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));
        return aResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()).orElse(null));
    }

    @Override
    public ClienteResponse obtenerPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con CURP: " + curp));
        return aResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()).orElse(null));
    }

    @Override
    public ClienteResponse obtenerPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con RFC: " + rfc));
        return aResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()).orElse(null));
    }

    @Override
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return aResponse(cuenta.getCliente(), cuenta);
    }

    @Override
    public List<ClienteResponse> obtenerTodos() {
        return clienteRepository.findAll().stream()
                .map(c -> aResponse(c, cuentaRepository.findByClienteId(c.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> obtenerActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(c -> aResponse(c, cuentaRepository.findByClienteId(c.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> obtenerPorRangoDeFechas(LocalDateTime desde, LocalDateTime hasta) {
        return clienteRepository.findByFechaRegistroBetween(desde, hasta).stream()
                .map(c -> aResponse(c, cuentaRepository.findByClienteId(c.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse actualizaCliente(Integer id, ActualizaClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));

        // CURP, RFC y numero de cuenta nunca se modifican (regla de negocio explicita)
        if (request.getNombre() != null) cliente.setNombre(request.getNombre());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre());
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(request.getApellidoPaterno());
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(request.getApellidoMaterno());
        if (request.getNacionalidad() != null) cliente.setNacionalidad(request.getNacionalidad());
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil());
        if (request.getCorreoElectronico() != null) cliente.setCorreoElectronico(request.getCorreoElectronico());
        if (request.getTelefonoMovil() != null) cliente.setTelefonoMovil(request.getTelefonoMovil());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        if (request.getOcupacion() != null) cliente.setOcupacion(request.getOcupacion());
        if (request.getEmpresa() != null) cliente.setEmpresa(request.getEmpresa());
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual());
        clienteRepository.save(cliente);

        domicilioRepository.findByClienteId(cliente.getId()).ifPresent(domicilio -> {
            if (request.getCalle() != null) domicilio.setCalle(request.getCalle());
            if (request.getNumeroExterior() != null) domicilio.setNumeroExterior(request.getNumeroExterior());
            if (request.getNumeroInterior() != null) domicilio.setNumeroInterior(request.getNumeroInterior());
            if (request.getColonia() != null) domicilio.setColonia(request.getColonia());
            if (request.getMunicipio() != null) domicilio.setMunicipio(request.getMunicipio());
            if (request.getEstado() != null) domicilio.setEstado(request.getEstado());
            if (request.getCodigoPostal() != null) domicilio.setCodigoPostal(request.getCodigoPostal());
            if (request.getPais() != null) domicilio.setPais(request.getPais());
            domicilioRepository.save(domicilio);
        });

        return aResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()).orElse(null));
    }

    @Override
    @Transactional
    public GenericResponse bajaLogica(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        cuentaRepository.findByClienteId(cliente.getId()).ifPresent(cuenta -> {
            cuenta.setEstatus("INACTIVA");
            cuentaRepository.save(cuenta);
        });

        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Cliente desactivado correctamente");
        return response;
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 anos o mas)");
        }
    }

    private void validarUnicidad(ClienteRequest request) {
        clienteRepository.findByCurp(request.getCurp())
                .ifPresent(c -> { throw new CurpDuplicadaException(request.getCurp()); });
        clienteRepository.findByRfc(request.getRfc())
                .ifPresent(c -> { throw new RfcDuplicadoException(request.getRfc()); });
        clienteRepository.findByCorreoElectronico(request.getCorreoElectronico())
                .ifPresent(c -> { throw new CorreoDuplicadoException(request.getCorreoElectronico()); });
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            numeroCuenta = String.valueOf(System.currentTimeMillis()).substring(1)
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } while (cuentaRepository.findByNumeroCuenta(numeroCuenta).isPresent());
        return numeroCuenta;
    }

    private ClienteResponse aResponse(Cliente cliente, Cuenta cuenta) {
        return new ClienteResponse(
                cliente.getId(), cliente.getNombre(), cliente.getSegundoNombre(),
                cliente.getApellidoPaterno(), cliente.getApellidoMaterno(), cliente.getFechaNacimiento(),
                cliente.getCurp(), cliente.getRfc(), cliente.getSexo(), cliente.getNacionalidad(),
                cliente.getEstadoCivil(), cliente.getCorreoElectronico(), cliente.getTelefonoMovil(),
                cliente.getTelefonoAlternativo(), cliente.getOcupacion(), cliente.getEmpresa(),
                cliente.getIngresoMensual(), cliente.getActivo(), cliente.getFechaRegistro(),
                cuenta != null ? cuenta.getNumeroCuenta() : null,
                cuenta != null ? cuenta.getSaldo() : null,
                cuenta != null ? cuenta.getEstatus() : null
        );
    }
}