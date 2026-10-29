package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Domicilio;
import com.proyecto.servicios.exception.clientes.*;
import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.BuscarClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.ClienteSpecs;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.DomicilioRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.util.CurpValidador;
import com.proyecto.servicios.util.GeneradorNumeroCuenta;
import com.proyecto.servicios.util.RfcValidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
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

    // ------------------------------------------------------------------ Crear

    @Override
    @Transactional
    public ClienteResponse creaCliente(ClienteRequest request) {
        validarMayoriaDeEdad(request.getFechaNacimiento());

        String curp = mayusculas(request.getCurp());
        String rfc = mayusculas(request.getRfc());
        String correo = minusculas(request.getCorreoElectronico());
        validarUnicidad(curp, rfc, correo);

        Cliente cliente = new Cliente();
        cliente.setNombre(limpiar(request.getNombre()));
        cliente.setSegundoNombre(vacioANull(request.getSegundoNombre()));
        cliente.setApellidoPaterno(limpiar(request.getApellidoPaterno()));
        cliente.setApellidoMaterno(limpiar(request.getApellidoMaterno()));
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(curp);
        cliente.setRfc(rfc);
        cliente.setSexo(mayusculas(request.getSexo()));
        cliente.setNacionalidad(mayusculas(request.getNacionalidad()));
        cliente.setEstadoCivil(limpiar(request.getEstadoCivil()));
        cliente.setCorreoElectronico(correo);
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(vacioANull(request.getTelefonoAlternativo()));
        cliente.setOcupacion(vacioANull(request.getOcupacion()));
        cliente.setEmpresa(vacioANull(request.getEmpresa()));
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);
        cliente = clienteRepository.save(cliente);

        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        domicilio.setCalle(limpiar(request.getCalle()));
        domicilio.setNumeroExterior(limpiar(request.getNumeroExterior()));
        domicilio.setNumeroInterior(vacioANull(request.getNumeroInterior()));
        domicilio.setColonia(limpiar(request.getColonia()));
        domicilio.setMunicipio(limpiar(request.getMunicipio()));
        domicilio.setEstado(limpiar(request.getEstado()));
        domicilio.setCodigoPostal(request.getCodigoPostal());
        domicilio.setPais(mayusculas(request.getPais()));
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

    // ---------------------------------------------------------------- Consultas

    @Override
    public ClienteResponse obtenerPorId(Integer id) {
        validarId(id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));
        return aResponse(cliente, cuentaDe(cliente));
    }

    @Override
    public List<ClienteResponse> obtenerTodos() {
        return clienteRepository.findAll().stream()
                .map(c -> aResponse(c, cuentaDe(c)))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------- Busqueda

    @Override
    public List<ClienteResponse> buscar(BuscarClienteRequest criterios) {
        if (criterios == null || sinCriterios(criterios)) {
            throw new ValidacionNegocioException(
                    "Debe enviar al menos un criterio de busqueda: id, nombre, curp, rfc, correoElectronico, "
                            + "numeroCuenta, sexo, nacionalidad, activo, fechaRegistroDesde o fechaRegistroHasta");
        }

        // Los formatos ya se validaron en el DTO (@Valid); aqui solo se normalizan los valores.
        String nombre = vacioANull(criterios.getNombre());
        String curp = Optional.ofNullable(vacioANull(criterios.getCurp())).map(CurpValidador::normalizar).orElse(null);
        String rfc = Optional.ofNullable(vacioANull(criterios.getRfc())).map(RfcValidador::normalizar).orElse(null);
        String correo = Optional.ofNullable(vacioANull(criterios.getCorreoElectronico())).map(ClienteServiceImpl::minusculas).orElse(null);
        String cuenta = Optional.ofNullable(vacioANull(criterios.getNumeroCuenta())).map(ClienteServiceImpl::mayusculas).orElse(null);
        String sexo = Optional.ofNullable(vacioANull(criterios.getSexo())).map(ClienteServiceImpl::mayusculas).orElse(null);
        String nacionalidad = Optional.ofNullable(vacioANull(criterios.getNacionalidad())).map(ClienteServiceImpl::mayusculas).orElse(null);

        // Consulta dinamica: cada criterio presente agrega un filtro, y todos se combinan con "y".
        Specification<Cliente> filtro = ClienteSpecs.todos();
        if (criterios.getId() != null) {
            filtro = filtro.and(ClienteSpecs.conId(criterios.getId()));
        }
        if (curp != null) {
            filtro = filtro.and(ClienteSpecs.conCurp(curp));
        }
        if (rfc != null) {
            filtro = filtro.and(ClienteSpecs.conRfc(rfc));
        }
        if (correo != null) {
            filtro = filtro.and(ClienteSpecs.conCorreo(correo));
        }
        if (cuenta != null) {
            filtro = filtro.and(ClienteSpecs.conNumeroCuenta(cuenta));
        }
        if (sexo != null) {
            filtro = filtro.and(ClienteSpecs.conSexo(sexo));
        }
        if (nacionalidad != null) {
            filtro = filtro.and(ClienteSpecs.conNacionalidad(nacionalidad));
        }
        if (criterios.getActivo() != null) {
            filtro = filtro.and(ClienteSpecs.conActivo(criterios.getActivo()));
        }
        if (criterios.getFechaRegistroDesde() != null) {
            filtro = filtro.and(ClienteSpecs.registradoDesde(criterios.getFechaRegistroDesde()));
        }
        if (criterios.getFechaRegistroHasta() != null) {
            filtro = filtro.and(ClienteSpecs.registradoHasta(criterios.getFechaRegistroHasta()));
        }
        if (nombre != null) {
            // Todas las palabras escritas deben aparecer en el nombre completo, sin importar acentos ni mayusculas.
            for (String palabra : palabrasDeBusqueda(nombre)) {
                filtro = filtro.and(ClienteSpecs.nombreContiene(palabra));
            }
        }

        List<ClienteResponse> resultado = clienteRepository.findAll(filtro, Sort.by("id")).stream()
                .map(c -> aResponse(c, cuentaDe(c)))
                .collect(Collectors.toList());

        if (resultado.isEmpty()) {
            throw new ClienteNoEncontradoException("No se encontraron clientes con: "
                    + describirCriterios(criterios, nombre, curp, rfc, correo, cuenta, sexo, nacionalidad));
        }
        return resultado;
    }

    private boolean sinCriterios(BuscarClienteRequest c) {
        return c.getId() == null && vacioANull(c.getNombre()) == null && vacioANull(c.getCurp()) == null
                && vacioANull(c.getRfc()) == null && vacioANull(c.getCorreoElectronico()) == null
                && vacioANull(c.getNumeroCuenta()) == null && vacioANull(c.getSexo()) == null
                && vacioANull(c.getNacionalidad()) == null && c.getActivo() == null
                && c.getFechaRegistroDesde() == null && c.getFechaRegistroHasta() == null;
    }

    /** Palabras del nombre buscado, en minusculas, sin acentos y solo con letras (nunca llegan comodines al LIKE). */
    private List<String> palabrasDeBusqueda(String nombre) {
        List<String> palabras = new ArrayList<>();
        for (String palabra : sinAcentos(nombre).split("\\s+")) {
            String limpia = palabra.replaceAll("[^a-z]", "");
            if (!limpia.isEmpty()) {
                palabras.add(limpia);
            }
        }
        return palabras;
    }

    private String describirCriterios(BuscarClienteRequest c, String nombre, String curp, String rfc,
                                      String correo, String cuenta, String sexo, String nacionalidad) {
        List<String> partes = new ArrayList<>();
        if (c.getId() != null) partes.add("id=" + c.getId());
        if (nombre != null) partes.add("nombre='" + nombre + "'");
        if (curp != null) partes.add("curp='" + curp + "'");
        if (rfc != null) partes.add("rfc='" + rfc + "'");
        if (correo != null) partes.add("correoElectronico='" + correo + "'");
        if (cuenta != null) partes.add("numeroCuenta='" + cuenta + "'");
        if (sexo != null) partes.add("sexo='" + sexo + "'");
        if (nacionalidad != null) partes.add("nacionalidad='" + nacionalidad + "'");
        if (c.getActivo() != null) partes.add("activo=" + c.getActivo());
        if (c.getFechaRegistroDesde() != null) partes.add("fechaRegistroDesde=" + c.getFechaRegistroDesde());
        if (c.getFechaRegistroHasta() != null) partes.add("fechaRegistroHasta=" + c.getFechaRegistroHasta());
        return String.join(", ", partes);
    }

    // -------------------------------------------------------------- Actualizar

    @Override
    @Transactional
    public ClienteResponse actualizaCliente(ActualizaClienteRequest request) {
        Integer id = request.getId();
        Cliente cliente = Optional.ofNullable(id)
                .flatMap(clienteRepository::findById)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));

        // El correo es unico: se valida que no lo use otro cliente antes de asignarlo
        Optional.ofNullable(request.getCorreoElectronico())
                .map(ClienteServiceImpl::minusculas)
                .ifPresent(correoNuevo -> {
                    clienteRepository.findByCorreoElectronico(correoNuevo)
                            .filter(otro -> !otro.getId().equals(cliente.getId()))
                            .ifPresent(otro -> {
                                throw new CorreoDuplicadoException(correoNuevo);
                            });
                    cliente.setCorreoElectronico(correoNuevo);
                });

        // Solo se asigna lo que viene en la peticion (Optional vacio = campo omitido = no se toca).
        // CURP, RFC, sexo, fecha de nacimiento y numero de cuenta nunca se modifican (regla de negocio).
        aplicar(request.getNombre(), ClienteServiceImpl::limpiar, cliente::setNombre);
        aplicar(request.getSegundoNombre(), ClienteServiceImpl::vacioANull, cliente::setSegundoNombre);
        aplicar(request.getApellidoPaterno(), ClienteServiceImpl::limpiar, cliente::setApellidoPaterno);
        aplicar(request.getApellidoMaterno(), ClienteServiceImpl::limpiar, cliente::setApellidoMaterno);
        aplicar(request.getNacionalidad(), ClienteServiceImpl::mayusculas, cliente::setNacionalidad);
        aplicar(request.getEstadoCivil(), ClienteServiceImpl::limpiar, cliente::setEstadoCivil);
        aplicar(request.getTelefonoMovil(), UnaryOperator.identity(), cliente::setTelefonoMovil);
        aplicar(request.getTelefonoAlternativo(), ClienteServiceImpl::vacioANull, cliente::setTelefonoAlternativo);
        aplicar(request.getOcupacion(), ClienteServiceImpl::vacioANull, cliente::setOcupacion);
        aplicar(request.getEmpresa(), ClienteServiceImpl::vacioANull, cliente::setEmpresa);
        Optional.ofNullable(request.getIngresoMensual()).ifPresent(cliente::setIngresoMensual);
        clienteRepository.save(cliente);

        domicilioRepository.findByClienteId(cliente.getId()).ifPresent(domicilio -> {
            aplicar(request.getCalle(), ClienteServiceImpl::limpiar, domicilio::setCalle);
            aplicar(request.getNumeroExterior(), ClienteServiceImpl::limpiar, domicilio::setNumeroExterior);
            aplicar(request.getNumeroInterior(), ClienteServiceImpl::vacioANull, domicilio::setNumeroInterior);
            aplicar(request.getColonia(), ClienteServiceImpl::limpiar, domicilio::setColonia);
            aplicar(request.getMunicipio(), ClienteServiceImpl::limpiar, domicilio::setMunicipio);
            aplicar(request.getEstado(), ClienteServiceImpl::limpiar, domicilio::setEstado);
            aplicar(request.getCodigoPostal(), UnaryOperator.identity(), domicilio::setCodigoPostal);
            aplicar(request.getPais(), ClienteServiceImpl::mayusculas, domicilio::setPais);
            domicilioRepository.save(domicilio);
        });

        return aResponse(cliente, cuentaDe(cliente));
    }

    /** Si el valor viene (Optional presente), lo transforma y lo asigna; si no viene, no hace nada. */
    private static void aplicar(String valor, UnaryOperator<String> transformacion, Consumer<String> asignar) {
        Optional.ofNullable(valor).ifPresent(v -> asignar.accept(transformacion.apply(v)));
    }

    // ------------------------------------------------------------- Baja logica

    @Override
    @Transactional
    public GenericResponse bajaLogica(Integer id) {
        validarId(id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("El cliente con id " + id + " ya se encuentra dado de baja");
        }
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        cuentaRepository.findByClienteId(cliente.getId()).ifPresent(cuenta -> {
            cuenta.setEstatus("INACTIVA");
            cuentaRepository.save(cuenta);
        });

        return GenericResponse.exito("Cliente desactivado correctamente");
    }

    // ---------------------------------------------------------------- Auxiliares

    private void validarId(Integer id) {
        if (id == null || id <= 0) {
            throw new ValidacionNegocioException("El id del cliente debe ser un numero entero positivo");
        }
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 anos o mas)");
        }
    }

    private void validarUnicidad(String curp, String rfc, String correo) {
        clienteRepository.findByCurp(curp)
                .ifPresent(c -> { throw new CurpDuplicadaException(curp); });
        clienteRepository.findByRfc(rfc)
                .ifPresent(c -> { throw new RfcDuplicadoException(rfc); });
        clienteRepository.findByCorreoElectronico(correo)
                .ifPresent(c -> { throw new CorreoDuplicadoException(correo); });
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            numeroCuenta = GeneradorNumeroCuenta.generar();
        } while (cuentaRepository.findByNumeroCuenta(numeroCuenta).isPresent());
        return numeroCuenta;
    }

    private Cuenta cuentaDe(Cliente cliente) {
        return cuentaRepository.findByClienteId(cliente.getId()).orElse(null);
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String vacioANull(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static String mayusculas(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private static String minusculas(String valor) {
        return valor == null ? null : valor.trim().toLowerCase(Locale.ROOT);
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