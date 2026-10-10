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
import com.proyecto.servicios.util.CurpValidador;
import com.proyecto.servicios.util.RfcValidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final int EDAD_MINIMA = 18;

    private static final String LETRAS =
            "A-Za-z\u00C1\u00C9\u00CD\u00D3\u00DA\u00DC\u00D1\u00E1\u00E9\u00ED\u00F3\u00FA\u00FC\u00F1";
    private static final Pattern PATRON_NOMBRE_BUSQUEDA = Pattern.compile("^[" + LETRAS + "]+( [" + LETRAS + "]+)*$");
    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PATRON_CUENTA = Pattern.compile("^[A-Za-z0-9]{6,30}$");

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
    public ClienteResponse obtenerPorCurp(String curp) {
        String normalizada = CurpValidador.normalizar(curp);
        CurpValidador.validar(normalizada).ifPresent(error -> {
            throw new ValidacionNegocioException(error);
        });
        Cliente cliente = clienteRepository.findByCurp(normalizada)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con CURP: " + normalizada));
        return aResponse(cliente, cuentaDe(cliente));
    }

    @Override
    public ClienteResponse obtenerPorRfc(String rfc) {
        String normalizado = RfcValidador.normalizar(rfc);
        RfcValidador.validar(normalizado).ifPresent(error -> {
            throw new ValidacionNegocioException(error);
        });
        Cliente cliente = clienteRepository.findByRfc(normalizado)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con RFC: " + normalizado));
        return aResponse(cliente, cuentaDe(cliente));
    }

    @Override
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        String numero = validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numero)
                .orElseThrow(() -> new CuentaNoEncontradaException(numero));
        return aResponse(cuenta.getCliente(), cuenta);
    }

    @Override
    public List<ClienteResponse> obtenerTodos() {
        return clienteRepository.findAll().stream()
                .map(c -> aResponse(c, cuentaDe(c)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> obtenerActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(c -> aResponse(c, cuentaDe(c)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> obtenerPorRangoDeFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionNegocioException("Las fechas 'desde' y 'hasta' son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        return clienteRepository.findByFechaRegistroBetween(desde, hasta).stream()
                .map(c -> aResponse(c, cuentaDe(c)))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------- Busqueda

    @Override
    public List<ClienteResponse> buscar(String nombre, String curp, String rfc, String correo,
                                        String numeroCuenta, Boolean activo) {
        String nombreBuscado = vacioANull(nombre);
        String curpBuscada = vacioANull(curp);
        String rfcBuscado = vacioANull(rfc);
        String correoBuscado = vacioANull(correo);
        String cuentaBuscada = vacioANull(numeroCuenta);

        if (nombreBuscado == null && curpBuscada == null && rfcBuscado == null
                && correoBuscado == null && cuentaBuscada == null && activo == null) {
            throw new ValidacionNegocioException(
                    "Debe enviar al menos un criterio de busqueda: nombre, curp, rfc, correo, numeroCuenta o activo");
        }

        // 1. Validar el formato de cada criterio antes de consultar
        if (nombreBuscado != null) {
            if (nombreBuscado.length() < 2 || nombreBuscado.length() > 100) {
                throw new ValidacionNegocioException("El nombre a buscar debe tener entre 2 y 100 caracteres");
            }
            if (!PATRON_NOMBRE_BUSQUEDA.matcher(nombreBuscado).matches()) {
                throw new ValidacionNegocioException(
                        "El nombre a buscar solo debe contener letras (sin numeros ni simbolos)");
            }
        }
        if (curpBuscada != null) {
            curpBuscada = CurpValidador.normalizar(curpBuscada);
            CurpValidador.validar(curpBuscada).ifPresent(error -> {
                throw new ValidacionNegocioException(error);
            });
        }
        if (rfcBuscado != null) {
            rfcBuscado = RfcValidador.normalizar(rfcBuscado);
            RfcValidador.validar(rfcBuscado).ifPresent(error -> {
                throw new ValidacionNegocioException(error);
            });
        }
        if (correoBuscado != null) {
            correoBuscado = correoBuscado.toLowerCase(Locale.ROOT);
            if (!PATRON_CORREO.matcher(correoBuscado).matches()) {
                throw new ValidacionNegocioException(
                        "El correo a buscar no tiene un formato valido (ejemplo: nombre@dominio.com)");
            }
        }
        if (cuentaBuscada != null) {
            cuentaBuscada = validarNumeroCuenta(cuentaBuscada);
        }

        // 2. Candidatos: se parte del criterio mas especifico
        List<Cliente> candidatos = obtenerCandidatos(nombreBuscado, curpBuscada, rfcBuscado,
                correoBuscado, cuentaBuscada, activo);

        // 3. Se aplican todos los criterios con "y"
        final String nombreFinal = nombreBuscado;
        final String curpFinal = curpBuscada;
        final String rfcFinal = rfcBuscado;
        final String correoFinal = correoBuscado;
        final String cuentaFinal = cuentaBuscada;

        List<ClienteResponse> resultado = candidatos.stream()
                .filter(c -> curpFinal == null || curpFinal.equalsIgnoreCase(c.getCurp()))
                .filter(c -> rfcFinal == null || rfcFinal.equalsIgnoreCase(c.getRfc()))
                .filter(c -> correoFinal == null || correoFinal.equalsIgnoreCase(c.getCorreoElectronico()))
                .filter(c -> activo == null || activo.equals(c.getActivo()))
                .filter(c -> nombreFinal == null || nombreCoincide(c, nombreFinal))
                .map(c -> new Object[] {c, cuentaDe(c)})
                .filter(par -> cuentaFinal == null
                        || (par[1] != null && cuentaFinal.equalsIgnoreCase(((Cuenta) par[1]).getNumeroCuenta())))
                .map(par -> aResponse((Cliente) par[0], (Cuenta) par[1]))
                .collect(Collectors.toList());

        if (resultado.isEmpty()) {
            throw new ClienteNoEncontradoException("No se encontraron clientes con: "
                    + describirCriterios(nombreBuscado, curpBuscada, rfcBuscado, correoBuscado, cuentaBuscada, activo));
        }
        return resultado;
    }

    private List<Cliente> obtenerCandidatos(String nombre, String curp, String rfc, String correo,
                                            String cuenta, Boolean activo) {
        if (curp != null) {
            return clienteRepository.findByCurp(curp).map(List::of).orElseGet(List::of);
        }
        if (rfc != null) {
            return clienteRepository.findByRfc(rfc).map(List::of).orElseGet(List::of);
        }
        if (correo != null) {
            return clienteRepository.findByCorreoElectronicoIgnoreCase(correo).map(List::of).orElseGet(List::of);
        }
        if (cuenta != null) {
            return cuentaRepository.findByNumeroCuenta(cuenta.toUpperCase(Locale.ROOT))
                    .map(c -> List.of(c.getCliente()))
                    .orElseGet(List::of);
        }
        if (Boolean.TRUE.equals(activo)) {
            return clienteRepository.findByActivoTrue();
        }
        // Nombre (sin acentos ni mayusculas) o activo=false: se filtra en memoria
        return clienteRepository.findAll();
    }

    /** Todas las palabras escritas deben aparecer en el nombre completo, sin importar acentos ni mayusculas. */
    private boolean nombreCoincide(Cliente cliente, String texto) {
        String completo = sinAcentos(String.join(" ",
                valorODefecto(cliente.getNombre()), valorODefecto(cliente.getSegundoNombre()),
                valorODefecto(cliente.getApellidoPaterno()), valorODefecto(cliente.getApellidoMaterno())));
        return Arrays.stream(sinAcentos(texto).split(" "))
                .filter(palabra -> !palabra.isBlank())
                .allMatch(completo::contains);
    }

    private String describirCriterios(String nombre, String curp, String rfc, String correo,
                                      String cuenta, Boolean activo) {
        List<String> partes = new ArrayList<>();
        if (nombre != null) partes.add("nombre='" + nombre + "'");
        if (curp != null) partes.add("curp='" + curp + "'");
        if (rfc != null) partes.add("rfc='" + rfc + "'");
        if (correo != null) partes.add("correo='" + correo + "'");
        if (cuenta != null) partes.add("numeroCuenta='" + cuenta + "'");
        if (activo != null) partes.add("activo=" + activo);
        return String.join(", ", partes);
    }

    // -------------------------------------------------------------- Actualizar

    @Override
    @Transactional
    public ClienteResponse actualizaCliente(Integer id, ActualizaClienteRequest request) {
        validarId(id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + id));

        if (request.getCorreoElectronico() != null) {
            String correoNuevo = minusculas(request.getCorreoElectronico());
            clienteRepository.findByCorreoElectronico(correoNuevo)
                    .filter(otro -> !otro.getId().equals(cliente.getId()))
                    .ifPresent(otro -> { throw new CorreoDuplicadoException(correoNuevo); });
            cliente.setCorreoElectronico(correoNuevo);
        }

        // CURP, RFC, sexo, fecha de nacimiento y numero de cuenta nunca se modifican (regla de negocio)
        if (request.getNombre() != null) cliente.setNombre(limpiar(request.getNombre()));
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(vacioANull(request.getSegundoNombre()));
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(limpiar(request.getApellidoPaterno()));
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(limpiar(request.getApellidoMaterno()));
        if (request.getNacionalidad() != null) cliente.setNacionalidad(mayusculas(request.getNacionalidad()));
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(limpiar(request.getEstadoCivil()));
        if (request.getTelefonoMovil() != null) cliente.setTelefonoMovil(request.getTelefonoMovil());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(vacioANull(request.getTelefonoAlternativo()));
        if (request.getOcupacion() != null) cliente.setOcupacion(vacioANull(request.getOcupacion()));
        if (request.getEmpresa() != null) cliente.setEmpresa(vacioANull(request.getEmpresa()));
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual());
        clienteRepository.save(cliente);

        domicilioRepository.findByClienteId(cliente.getId()).ifPresent(domicilio -> {
            if (request.getCalle() != null) domicilio.setCalle(limpiar(request.getCalle()));
            if (request.getNumeroExterior() != null) domicilio.setNumeroExterior(limpiar(request.getNumeroExterior()));
            if (request.getNumeroInterior() != null) domicilio.setNumeroInterior(vacioANull(request.getNumeroInterior()));
            if (request.getColonia() != null) domicilio.setColonia(limpiar(request.getColonia()));
            if (request.getMunicipio() != null) domicilio.setMunicipio(limpiar(request.getMunicipio()));
            if (request.getEstado() != null) domicilio.setEstado(limpiar(request.getEstado()));
            if (request.getCodigoPostal() != null) domicilio.setCodigoPostal(request.getCodigoPostal());
            if (request.getPais() != null) domicilio.setPais(mayusculas(request.getPais()));
            domicilioRepository.save(domicilio);
        });

        return aResponse(cliente, cuentaDe(cliente));
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

        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Cliente desactivado correctamente");
        return response;
    }

    // ---------------------------------------------------------------- Auxiliares

    private void validarId(Integer id) {
        if (id == null || id <= 0) {
            throw new ValidacionNegocioException("El id del cliente debe ser un numero entero positivo");
        }
    }

    private String validarNumeroCuenta(String numeroCuenta) {
        String numero = numeroCuenta == null ? "" : numeroCuenta.trim().toUpperCase(Locale.ROOT);
        if (!PATRON_CUENTA.matcher(numero).matches()) {
            throw new ValidacionNegocioException(
                    "El numero de cuenta debe tener entre 6 y 30 caracteres, solo letras y numeros");
        }
        return numero;
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
            numeroCuenta = String.valueOf(System.currentTimeMillis()).substring(1)
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
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

    private static String valorODefecto(String valor) {
        return valor == null ? "" : valor;
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