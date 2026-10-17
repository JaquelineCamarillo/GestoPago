package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Filtro opcional del catalogo de productos. Sin tipoFront se devuelve todo el catalogo. */
@Getter
@Setter
@NoArgsConstructor
public class ProductoFiltroRequest {

    @PositiveOrZero(message = "El tipoFront debe ser un numero entero igual o mayor a cero")
    @Schema(example = "2", description = "Tipo de producto (1, 2, 4, 5, 30, 31...). Omitirlo devuelve todo el catalogo")
    private Integer tipoFront;
}