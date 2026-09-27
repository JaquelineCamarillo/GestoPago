package com.proyecto.servicios.exception.clientes;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correo) {
        super("Ya existe un cliente registrado con el correo: " + correo);
    }
}