package com.proyecto.servicios.exception.clientes;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No existe una cuenta con el numero: " + numeroCuenta);
    }
}