package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ClienteIdRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.LoginFacialRequest;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.RegistrarCredencialesRequest;
import com.proyecto.servicios.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Login", description = "Credenciales, inicio y cierre de sesion")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(value = "/registrar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registra la contrasena y el rostro de un cliente")
    public ResponseEntity<GenericResponse> registrarCredenciales(@Valid @RequestBody RegistrarCredencialesRequest request) {
        return ResponseEntity.ok(loginService.registrarCredenciales(request));
    }

    @PostMapping(value = "/iniciar-sesion", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Inicia sesion con correo y contrasena")
    public ResponseEntity<GenericResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Inicio de sesion correcto",
                loginService.iniciarSesion(request)));
    }

    @PostMapping(value = "/iniciar-sesion-facial", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Inicia sesion con reconocimiento facial")
    public ResponseEntity<GenericResponse> iniciarSesionFacial(@Valid @RequestBody LoginFacialRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Inicio de sesion correcto",
                loginService.iniciarSesionFacial(request)));
    }

    @PostMapping(value = "/cerrar-sesion", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cierra la sesion de un cliente")
    public ResponseEntity<GenericResponse> cerrarSesion(@Valid @RequestBody ClienteIdRequest request) {
        return ResponseEntity.ok(loginService.cerrarSesion(request.getId()));
    }

    @PutMapping(value = "/actividad", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registra actividad del cliente para mantener su sesion abierta")
    public ResponseEntity<GenericResponse> registrarActividad(@Valid @RequestBody ClienteIdRequest request) {
        loginService.registrarActividad(request.getId());
        return ResponseEntity.ok(GenericResponse.exito("Actividad registrada correctamente"));
    }
}