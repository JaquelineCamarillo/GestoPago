package com.proyecto.servicios.exception.gestopago;

/** Se lanza cuando no existe un token activo/valido y tampoco fue posible obtener uno nuevo. */
public class GestoPagoTokenUnavailableException extends GestoPagoIntegrationException {
    public GestoPagoTokenUnavailableException(String message) {
        super(message);
    }

    public GestoPagoTokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}