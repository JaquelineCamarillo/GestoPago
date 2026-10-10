package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.creaCliente(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> obtenerTodos() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    /**
     * Busqueda por uno o varios criterios (se combinan con "y").
     * Ejemplos: /clientes/buscar?nombre=juana camarillo  |  /clientes/buscar?rfc=CAOJ950518AB9&activo=true
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponse>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String numeroCuenta,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(clienteService.buscar(nombre, curp, rfc, correo, numeroCuenta, activo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @GetMapping("/curp/{curp}")
    public ResponseEntity<ClienteResponse> obtenerPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<ClienteResponse> obtenerPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc));
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<ClienteResponse> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> obtenerActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }

    /** Fechas en formato yyyy/MM/dd (ejemplo: desde=2026/01/01&hasta=2026/12/31). */
    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoDeFechas(
            @RequestParam @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate hasta) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoDeFechas(
                desde.atStartOfDay(), hasta.atTime(LocalTime.MAX)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @PathVariable Integer id, @Valid @RequestBody ActualizaClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizaCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> bajaLogica(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.bajaLogica(id));
    }
}