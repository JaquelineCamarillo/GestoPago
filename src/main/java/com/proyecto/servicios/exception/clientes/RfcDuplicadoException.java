package com.proyecto.servicios.exception.clientes;

public class RfcDuplicadoException extends RuntimeException {
    public RfcDuplicadoException(String rfc) {
        super("Ya existe un cliente registrado con el RFC: " + rfc);
    }
}