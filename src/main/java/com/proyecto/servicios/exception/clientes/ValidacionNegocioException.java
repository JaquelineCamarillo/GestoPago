package com.proyecto.servicios.exception.clientes;

public class ValidacionNegocioException extends RuntimeException {
    public ValidacionNegocioException(String message) {
        super(message);
    }
}