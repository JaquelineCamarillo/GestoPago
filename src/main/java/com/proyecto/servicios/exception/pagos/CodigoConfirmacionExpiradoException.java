package com.proyecto.servicios.exception.pagos;

public class CodigoConfirmacionExpiradoException extends RuntimeException {
    public CodigoConfirmacionExpiradoException(String mensaje) {
        super(mensaje);
    }
}
