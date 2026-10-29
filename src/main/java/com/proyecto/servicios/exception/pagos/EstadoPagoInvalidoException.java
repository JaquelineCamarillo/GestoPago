package com.proyecto.servicios.exception.pagos;

public class EstadoPagoInvalidoException extends RuntimeException {
    public EstadoPagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
