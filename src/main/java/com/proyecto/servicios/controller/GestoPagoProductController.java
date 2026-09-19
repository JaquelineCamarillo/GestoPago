package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gestopago")
public class GestoPagoProductController {

    private final GestoPagoProductService gestoPagoProductService;

    public GestoPagoProductController(GestoPagoProductService gestoPagoProductService) {
        this.gestoPagoProductService = gestoPagoProductService;
    }

    /**
     * Lee el catalogo desde cache (Redis, con Postgres como respaldo). Si se manda
     * ?tipoFront=1 solo regresa los productos de ese tipo; sin el parametro, regresa todo.
     */
    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GestoPagoProductoResponse>> obtenerProductos(
            @RequestParam(value = "tipoFront", required = false) Integer tipoFront) {
        return ResponseEntity.ok(gestoPagoProductService.listarProductosDisponibles(tipoFront));
    }

    @PostMapping("/productos/sincronizar")
    public ResponseEntity<Void> sincronizarProductos() {
        gestoPagoProductService.sincronizarCatalogo();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}