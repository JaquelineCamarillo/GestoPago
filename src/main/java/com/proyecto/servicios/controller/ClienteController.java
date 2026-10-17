package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.BuscarClienteRequest;
import com.proyecto.servicios.model.ClienteIdRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los datos siempre viajan en el cuerpo de la peticion (@RequestBody + @Valid): la API no usa
 * @PathVariable ni @RequestParam. Las validaciones viven en los DTO y los errores los procesa
 * GlobalExceptionHandler, por eso aqui no hay validaciones repetidas ni try-catch.
 */
@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clientes", description = "Alta, consulta, modificacion y baja de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registra un cliente (crea tambien su domicilio y su cuenta)")
    public ResponseEntity<GenericResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito("Cliente registrado correctamente", clienteService.creaCliente(request)));
    }

    @GetMapping
    @Operation(summary = "Lista todos los clientes")
    public ResponseEntity<GenericResponse> obtenerTodos() {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente", clienteService.obtenerTodos()));
    }

    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca clientes combinando uno o varios criterios",
            description = "Unico endpoint de consulta. Criterios opcionales (al menos uno): id, nombre, curp, rfc, "
                    + "correoElectronico, numeroCuenta, sexo, nacionalidad, activo, fechaRegistroDesde y "
                    + "fechaRegistroHasta. Los que se envian se combinan con 'y'.")
    public ResponseEntity<GenericResponse> buscar(@Valid @RequestBody BuscarClienteRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente", clienteService.buscar(request)));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifica los datos de un cliente",
            description = "Se envia el id y solo los campos a cambiar. CURP, RFC, sexo, fecha de nacimiento y "
                    + "numero de cuenta no se pueden modificar.")
    public ResponseEntity<GenericResponse> actualizarCliente(@Valid @RequestBody ActualizaClienteRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Cliente actualizado correctamente",
                clienteService.actualizaCliente(request)));
    }

    @PutMapping(value = "/baja", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Da de baja (logica) a un cliente y deja inactiva su cuenta")
    public ResponseEntity<GenericResponse> bajaLogica(@Valid @RequestBody ClienteIdRequest request) {
        return ResponseEntity.ok(clienteService.bajaLogica(request.getId()));
    }
}