package com.proyecto.servicios.exception.clientes;

public class LoginNoEncontradoException extends RuntimeException {
    public LoginNoEncontradoException(Integer clienteId) {
        super("El cliente con id " + clienteId + " no tiene credenciales registradas");
    }
}