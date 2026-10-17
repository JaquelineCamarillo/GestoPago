package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.Genero;
import com.proyecto.servicios.model.CatalogoItem;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.NacionalidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = "/catalogos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Catalogos", description = "Listas de valores permitidos para los formularios")
public class CatalogoController {

    private final NacionalidadService nacionalidadService;

    public CatalogoController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    @GetMapping("/generos")
    @Operation(summary = "Lista los generos permitidos al registrar un cliente")
    public ResponseEntity<GenericResponse> listarGeneros() {
        List<CatalogoItem> generos = Arrays.stream(Genero.values())
                .map(g -> new CatalogoItem(g.getCodigo(), g.getEtiqueta()))
                .toList();
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente", generos));
    }

    @GetMapping("/nacionalidades")
    @Operation(summary = "Lista las nacionalidades (paises) permitidas al registrar un cliente")
    public ResponseEntity<GenericResponse> listarNacionalidades() {
        return ResponseEntity.ok(GenericResponse.exito("Consulta realizada correctamente",
                nacionalidadService.listar()));
    }
}