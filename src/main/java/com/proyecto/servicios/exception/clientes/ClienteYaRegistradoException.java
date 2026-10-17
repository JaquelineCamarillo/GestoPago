package com.proyecto.servicios.exception.clientes;

public class ClienteYaRegistradoException extends RuntimeException {
    public ClienteYaRegistradoException(String message) {
        super(message);
    }
}