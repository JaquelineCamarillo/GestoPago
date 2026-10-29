package com.proyecto.servicios.exception.pagos;

public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException(Integer pagoId) {
        super("No existe un pago registrado con id: " + pagoId);
    }
}
