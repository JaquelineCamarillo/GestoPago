package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CuentaConsultaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cuentas", description = "Consulta de cuentas y saldos")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta una cuenta por su numero de cuenta")
    public ResponseEntity<GenericResponse> obtenerPorNumeroCuenta(@Valid @RequestBody CuentaConsultaRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente",
                cuentaService.obtenerPorNumeroCuenta(request.getNumeroCuenta())));
    }

    @GetMapping("/activas")
    @Operation(summary = "Lista las cuentas activas")
    public ResponseEntity<GenericResponse> obtenerActivas() {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente",
                cuentaService.obtenerActivas()));
    }

    @PostMapping(value = "/saldo", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta el saldo de una cuenta")
    public ResponseEntity<GenericResponse> consultarSaldo(@Valid @RequestBody CuentaConsultaRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente",
                cuentaService.consultarSaldo(request.getNumeroCuenta())));
    }
}