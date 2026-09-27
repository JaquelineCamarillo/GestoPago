package com.proyecto.servicios.exception.gestopago;

/** Se lanza cuando GestoPago no responde dentro del tiempo configurado. */
public class GestoPagoTimeoutException extends GestoPagoIntegrationException {
    public GestoPagoTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}