package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.Genero;
import com.proyecto.servicios.model.CatalogoItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/catalogos")
@Tag(name = "Catalogos", description = "Listas de valores permitidos para los formularios")
public class CatalogoController {

    @GetMapping("/generos")
    @Operation(summary = "Lista los generos permitidos al registrar un cliente")
    public List<CatalogoItem> listarGeneros() {
        return Arrays.stream(Genero.values())
                .map(g -> new CatalogoItem(g.getCodigo(), g.getEtiqueta()))
                .toList();
    }
}