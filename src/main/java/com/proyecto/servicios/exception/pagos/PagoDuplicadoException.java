package com.proyecto.servicios.exception.pagos;

public class PagoDuplicadoException extends RuntimeException {
    public PagoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
