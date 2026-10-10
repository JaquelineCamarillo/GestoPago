package com.proyecto.servicios.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.enums.GestoPagoCodigoResultado;
import com.proyecto.servicios.exception.clientes.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.clientes.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.clientes.CorreoDuplicadoException;
import com.proyecto.servicios.exception.clientes.CredencialesInvalidasException;
import com.proyecto.servicios.exception.clientes.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.clientes.CurpDuplicadaException;
import com.proyecto.servicios.exception.clientes.LoginNoEncontradoException;
import com.proyecto.servicios.exception.clientes.RfcDuplicadoException;
import com.proyecto.servicios.exception.clientes.ValidacionNegocioException;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoBadResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTokenUnavailableException;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Set<Class<?>> TIPOS_ENTEROS = Set.of(
            Integer.class, int.class, Long.class, long.class, Short.class, short.class);
    private static final Set<Class<?>> TIPOS_DECIMALES = Set.of(
            Double.class, double.class, Float.class, float.class, BigDecimal.class);

    // ---------------------------------------------------------------- GestoPago

    @ExceptionHandler(GestoPagoAuthenticationException.class)
    public ResponseEntity<GenericResponse> handleAutenticacion(GestoPagoAuthenticationException ex) {
        return construir(mapearMotivo(ex.getMotivo()), ex.getMessage());
    }

    @ExceptionHandler(GestoPagoTokenUnavailableException.class)
    public ResponseEntity<GenericResponse> handleTokenNoDisponible(GestoPagoTokenUnavailableException ex) {
        return construir(GestoPagoCodigoResultado.TOKEN_NO_DISPONIBLE, ex.getMessage());
    }

    @ExceptionHandler(GestoPagoTimeoutException.class)
    public ResponseEntity<GenericResponse> handleTimeout(GestoPagoTimeoutException ex) {
        return construir(GestoPagoCodigoResultado.TIMEOUT, ex.getMessage());
    }

    @ExceptionHandler(GestoPagoCommunicationException.class)
    public ResponseEntity<GenericResponse> handleComunicacion(GestoPagoCommunicationException ex) {
        return construir(GestoPagoCodigoResultado.ERROR_COMUNICACION, ex.getMessage());
    }

    @ExceptionHandler(GestoPagoBadResponseException.class)
    public ResponseEntity<GenericResponse> handleRespuestaNoExitosa(GestoPagoBadResponseException ex) {
        return construir(GestoPagoCodigoResultado.RESPUESTA_NO_EXITOSA, ex.getMessage());
    }

    // ---------------------------------------------------------------- Clientes

    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class,
            LoginNoEncontradoException.class})
    public ResponseEntity<GenericResponse> handleNoEncontrado(Exception ex) {
        return construirConEstatus(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({CurpDuplicadaException.class, RfcDuplicadoException.class,
            CorreoDuplicadoException.class, ClienteYaRegistradoException.class})
    public ResponseEntity<GenericResponse> handleDuplicado(Exception ex) {
        return construirConEstatus(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<GenericResponse> handleValidacionNegocio(ValidacionNegocioException ex) {
        return construirConEstatus(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredenciales(CredencialesInvalidasException ex) {
        return construirConEstatus(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ---------------------------------------------------- Validacion de entrada

    /** Campos que incumplen las anotaciones de validacion (@NotBlank, @Pattern, @Past...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidacionCampos(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construirConEstatus(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Validaciones sobre parametros de ruta o query (@Validated en el controlador). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleRestricciones(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(v -> ultimoNodo(v.getPropertyPath().toString()) + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return construirConEstatus(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** JSON mal armado, vacio, o con un valor del tipo equivocado (texto en un numero, fecha invalida...). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getCause();
        String mensaje;
        if (causa instanceof InvalidFormatException ife) {
            mensaje = mensajeDeTipo(nombreCampo(ife), ife.getTargetType(), "debe ser");
        } else if (causa instanceof MismatchedInputException mie) {
            mensaje = mensajeDeTipo(nombreCampo(mie), mie.getTargetType(), "tiene un formato incorrecto; se esperaba");
        } else if (causa instanceof JsonProcessingException) {
            mensaje = "El cuerpo de la peticion no es un JSON valido. Revisa llaves, comas y comillas.";
        } else {
            mensaje = "No se pudo leer el cuerpo de la peticion. Verifica que no este vacio y que sea un JSON valido.";
        }
        return construirConEstatus(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Parametro de ruta o query con tipo incorrecto (ej. /clientes/abc cuando el id es numerico). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parametro '" + ex.getName() + "' debe ser " + describirTipo(ex.getRequiredType()) + ".";
        return construirConEstatus(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponse> handleParametroFaltante(MissingServletRequestParameterException ex) {
        String mensaje = "Falta el parametro obligatorio '" + ex.getParameterName()
                + "' (debe ser " + describirTipo(tipoPorNombre(ex.getParameterType())) + ").";
        return construirConEstatus(HttpStatus.BAD_REQUEST, mensaje);
    }

    // ------------------------------------------------------------ Errores HTTP

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleRutaInexistente(NoResourceFoundException ex) {
        return construirConEstatus(HttpStatus.NOT_FOUND, "La ruta solicitada no existe: /" + ex.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        String permitidos = ex.getSupportedHttpMethods() == null ? "ninguno"
                : ex.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.joining(", "));
        String mensaje = "El metodo " + ex.getMethod() + " no esta permitido en esta ruta. Metodos permitidos: "
                + permitidos + ".";
        return construirConEstatus(HttpStatus.METHOD_NOT_ALLOWED, mensaje);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleTipoContenido(HttpMediaTypeNotSupportedException ex) {
        String recibido = ex.getContentType() == null ? "ninguno" : ex.getContentType().toString();
        return construirConEstatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de contenido no soportado (" + recibido + "). Envia el cuerpo como application/json.");
    }

    /** Respaldo de la base de datos: unicidad, longitudes, nulos que se escaparon a las validaciones. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleIntegridadDatos(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad en base de datos: {}", ex.getMostSpecificCause().getMessage());
        return construirConEstatus(HttpStatus.CONFLICT,
                "Los datos chocan con una restriccion de la base de datos (por ejemplo, un valor que ya existe "
                        + "como CURP, RFC, correo o numero de cuenta). Revisa la informacion enviada.");
    }

    // ---------------------------------------------------------------- Generico

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenerico(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return construir(GestoPagoCodigoResultado.ERROR_INTERNO,
                "Ocurrio un error inesperado. Contacte al administrador si el problema persiste.");
    }

    // ---------------------------------------------------------------- Auxiliares

    private String mensajeDeTipo(String campo, Class<?> tipo, String verbo) {
        if (campo.isBlank()) {
            return "El cuerpo de la peticion tiene un formato incorrecto; se esperaba un objeto JSON.";
        }
        return "El campo '" + campo + "' " + verbo + " " + describirTipo(tipo) + ".";
    }

    private String nombreCampo(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private String describirTipo(Class<?> tipo) {
        if (tipo == null) {
            return "un valor del tipo correcto";
        }
        if (LocalDate.class.equals(tipo)) {
            return "una fecha con formato yyyy/MM/dd (ejemplo: 1983/05/18)";
        }
        if (TIPOS_ENTEROS.contains(tipo)) {
            return "un numero entero (sin letras ni decimales)";
        }
        if (TIPOS_DECIMALES.contains(tipo)) {
            return "un numero (puede llevar decimales, sin letras)";
        }
        if (Boolean.class.equals(tipo) || boolean.class.equals(tipo)) {
            return "true o false";
        }
        if (tipo.isEnum()) {
            return "uno de estos valores: " + Arrays.toString(tipo.getEnumConstants());
        }
        if (String.class.equals(tipo)) {
            return "texto";
        }
        return "un valor de tipo " + tipo.getSimpleName();
    }

    private Class<?> tipoPorNombre(String nombre) {
        return switch (nombre == null ? "" : nombre) {
            case "Integer", "int" -> Integer.class;
            case "Long", "long" -> Long.class;
            case "Double", "double" -> Double.class;
            case "Boolean", "boolean" -> Boolean.class;
            case "LocalDate" -> LocalDate.class;
            case "String" -> String.class;
            default -> null;
        };
    }

    private String ultimoNodo(String ruta) {
        int punto = ruta.lastIndexOf('.');
        return punto >= 0 ? ruta.substring(punto + 1) : ruta;
    }

    private GestoPagoCodigoResultado mapearMotivo(GestoPagoAuthenticationException.Motivo motivo) {
        return switch (motivo) {
            case USUARIO_O_DISTRIBUIDOR_INVALIDO -> GestoPagoCodigoResultado.USUARIO_O_DISTRIBUIDOR_INVALIDO;
            case PASSWORD_INVALIDO -> GestoPagoCodigoResultado.PASSWORD_INVALIDO;
            case DISPOSITIVO_INVALIDO -> GestoPagoCodigoResultado.DISPOSITIVO_INVALIDO;
            case TOKEN_EXPIRADO -> GestoPagoCodigoResultado.TOKEN_EXPIRADO;
            default -> GestoPagoCodigoResultado.ERROR_INTERNO;
        };
    }

    private ResponseEntity<GenericResponse> construir(GestoPagoCodigoResultado codigo, String mensaje) {
        GenericResponse body = new GenericResponse();
        body.setCodigo(codigo.getCodigo());
        body.setMensaje(mensaje != null ? mensaje : codigo.getMensajePorDefecto());
        return ResponseEntity.status(codigo.getHttpStatus()).body(body);
    }

    private ResponseEntity<GenericResponse> construirConEstatus(HttpStatus status, String mensaje) {
        GenericResponse body = new GenericResponse();
        body.setCodigo(status.value());
        body.setMensaje(mensaje);
        return ResponseEntity.status(status).body(body);
    }
}