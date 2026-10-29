package com.proyecto.servicios.exception.pagos;

public class ProductoCatalogoException extends RuntimeException {
    private final boolean noEncontrado;

    public ProductoCatalogoException(String mensaje, boolean noEncontrado) {
        super(mensaje);
        this.noEncontrado = noEncontrado;
    }

    public boolean isNoEncontrado() {
        return noEncontrado;
    }
}
