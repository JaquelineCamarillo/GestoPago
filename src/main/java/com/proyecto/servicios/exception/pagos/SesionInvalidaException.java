package com.proyecto.servicios.exception.pagos;

public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
