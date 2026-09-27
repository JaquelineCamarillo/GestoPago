package com.proyecto.servicios.exception.gestopago;

/** Se lanza ante fallas de comunicacion con GestoPago (host caido, error 5xx, etc.). */
public class GestoPagoCommunicationException extends GestoPagoIntegrationException {
    public GestoPagoCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}