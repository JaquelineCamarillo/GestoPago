package com.proyecto.servicios.exception.pagos;

public class OperacionNoAutorizadaException extends RuntimeException {
    public OperacionNoAutorizadaException(String mensaje) {
        super(mensaje);
    }
}
