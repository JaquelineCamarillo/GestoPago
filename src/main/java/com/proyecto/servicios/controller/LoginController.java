package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.*;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(value = "/registrar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> registrarCredenciales(@Valid @RequestBody RegistrarCredencialesRequest request) {
        return ResponseEntity.ok(loginService.registrarCredenciales(request));
    }

    @PostMapping(value = "/iniciar-sesion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.iniciarSesion(request));
    }

    @PostMapping(value = "/iniciar-sesion-facial", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> iniciarSesionFacial(@Valid @RequestBody LoginFacialRequest request) {
        return ResponseEntity.ok(loginService.iniciarSesionFacial(request));
    }

    @PostMapping("/cerrar-sesion/{clienteId}")
    public ResponseEntity<GenericResponse> cerrarSesion(@PathVariable Integer clienteId) {
        return ResponseEntity.ok(loginService.cerrarSesion(clienteId));
    }

    @PutMapping("/actividad/{clienteId}")
    public ResponseEntity<Void> registrarActividad(@PathVariable Integer clienteId) {
        loginService.registrarActividad(clienteId);
        return ResponseEntity.noContent().build();
    }
}