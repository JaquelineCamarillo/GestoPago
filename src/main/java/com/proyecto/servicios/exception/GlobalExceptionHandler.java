package com.proyecto.servicios.exception;

import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoBadResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTokenUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de errores: cada motivo especifico de GestoPago se mapea a un
 * status y un mensaje claro, en vez de responder siempre un 400/500 generico.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacion(GestoPagoAuthenticationException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "GESTOPAGO_AUTENTICACION_" + ex.getMotivo().name(), ex.getMessage());
    }

    @ExceptionHandler(GestoPagoTokenUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTokenNoDisponible(GestoPagoTokenUnavailableException ex) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, "GESTOPAGO_TOKEN_NO_DISPONIBLE", ex.getMessage());
    }

    @ExceptionHandler(GestoPagoTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(GestoPagoTimeoutException ex) {
        return construir(HttpStatus.GATEWAY_TIMEOUT, "GESTOPAGO_TIMEOUT", ex.getMessage());
    }

    @ExceptionHandler(GestoPagoCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleComunicacion(GestoPagoCommunicationException ex) {
        return construir(HttpStatus.BAD_GATEWAY, "GESTOPAGO_ERROR_COMUNICACION", ex.getMessage());
    }

    @ExceptionHandler(GestoPagoBadResponseException.class)
    public ResponseEntity<ErrorResponse> handleRespuestaNoExitosa(GestoPagoBadResponseException ex) {
        return construir(HttpStatus.BAD_GATEWAY, "GESTOPAGO_RESPUESTA_NO_EXITOSA", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrio un error inesperado. Contacte al administrador si el problema persiste.");
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String error, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), error, mensaje));
    }
}