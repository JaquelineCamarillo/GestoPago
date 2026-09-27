package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
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

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> obtenerActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }

    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoDeFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoDeFechas(desde, hasta));
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