package com.proyecto.servicios.exception.pagos;

public class CodigoConfirmacionInvalidoException extends RuntimeException {
    public CodigoConfirmacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
