package com.proyecto.servicios.exception.gestopago;

/** Excepcion raiz para cualquier falla al integrar con el servicio externo GestoPago. */
public class GestoPagoIntegrationException extends RuntimeException {

    public GestoPagoIntegrationException(String message) {
        super(message);
    }

    public GestoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}