package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.ProductoFiltroRequest;
import com.proyecto.servicios.service.GestoPagoProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/gestopago", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "GestoPago", description = "Catalogo de productos de GestoPago (PuntoRed)")
public class GestoPagoProductController {

    private final GestoPagoProductService gestoPagoProductService;

    public GestoPagoProductController(GestoPagoProductService gestoPagoProductService) {
        this.gestoPagoProductService = gestoPagoProductService;
    }

    /** Catalogo completo, ordenado por precio ascendente. Se lee de cache (Redis, con Postgres como respaldo). */
    @GetMapping("/productos")
    @Operation(summary = "Lista todo el catalogo de productos ordenado por precio ascendente")
    public ResponseEntity<GenericResponse> obtenerProductos() {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente",
                gestoPagoProductService.listarProductosDisponibles(null)));
    }

    /** Catalogo filtrado: el tipoFront viaja en el cuerpo; si se omite, regresa todo el catalogo. */
    @PostMapping(value = "/productos/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta el catalogo filtrando por tipoFront (opcional)")
    public ResponseEntity<GenericResponse> consultarProductos(
            @Valid @RequestBody(required = false) ProductoFiltroRequest filtro) {
        Integer tipoFront = filtro == null ? null : filtro.getTipoFront();
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente",
                gestoPagoProductService.listarProductosDisponibles(tipoFront)));
    }

    @PostMapping("/productos/sincronizar")
    @Operation(summary = "Sincroniza el catalogo desde GestoPago (solo para pruebas; el cron lo hace a diario)")
    public ResponseEntity<GenericResponse> sincronizarProductos() {
        gestoPagoProductService.sincronizarCatalogo();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GenericResponse.exito("Sincronizacion del catalogo realizada correctamente"));
    }
}