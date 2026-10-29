package com.proyecto.servicios.exception.pagos;

public class IntentosCodigoAgotadosException extends RuntimeException {
    public IntentosCodigoAgotadosException(String mensaje) {
        super(mensaje);
    }
}
