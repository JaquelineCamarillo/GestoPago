package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.GestoPagoCodigoResultado;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoBadResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTokenUnavailableException;
import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenerico(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return construir(GestoPagoCodigoResultado.ERROR_INTERNO,
                "Ocurrio un error inesperado. Contacte al administrador si el problema persiste.");
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
}