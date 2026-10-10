package com.proyecto.servicios.exception.clientes;

/** El correo con el que se intenta iniciar sesion no pertenece a ningun cliente. */
public class UsuarioNoEncontradoException extends CredencialesInvalidasException {
    public UsuarioNoEncontradoException(String correo) {
        super("El correo electronico '" + correo + "' no esta registrado");
    }
}