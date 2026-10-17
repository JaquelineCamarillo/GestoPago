package com.proyecto.servicios.exception.clientes;

/** El correo existe pero la contrasena no coincide. */
public class PasswordIncorrectaException extends CredencialesInvalidasException {
    public PasswordIncorrectaException() {
        super("La contrasena es incorrecta");
    }
}