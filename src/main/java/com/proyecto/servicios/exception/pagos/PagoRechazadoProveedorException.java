package com.proyecto.servicios.exception.pagos;

public class PagoRechazadoProveedorException extends RuntimeException {
    public PagoRechazadoProveedorException(String mensaje) {
        super(mensaje);
    }
}
