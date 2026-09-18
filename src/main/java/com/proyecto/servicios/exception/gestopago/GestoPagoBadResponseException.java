package com.proyecto.servicios.exception.gestopago;

import lombok.Getter;

/** Se lanza cuando GestoPago responde 200 OK pero el CODIGO de negocio no es "01" (exito). */
@Getter
public class GestoPagoBadResponseException extends GestoPagoIntegrationException {

    private final String codigoGestoPago;

    public GestoPagoBadResponseException(String message, String codigoGestoPago) {
        super(message);
        this.codigoGestoPago = codigoGestoPago;
    }
}