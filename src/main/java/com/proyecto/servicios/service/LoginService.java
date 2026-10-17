package com.proyecto.servicios.service;

import com.proyecto.servicios.model.*;

public interface LoginService {
    GenericResponse registrarCredenciales(RegistrarCredencialesRequest request);
    LoginResponse iniciarSesion(LoginRequest request);
    LoginResponse iniciarSesionFacial(LoginFacialRequest request);
    GenericResponse cerrarSesion(Integer clienteId);
    void registrarActividad(Integer clienteId);
}