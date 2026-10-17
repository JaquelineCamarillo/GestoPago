package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identifica a un cliente en el cuerpo de la peticion (la API no usa @PathVariable ni @RequestParam).
 * Se usa en la baja logica y en las operaciones de sesion.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteIdRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero entero positivo")
    @Schema(example = "1")
    private Integer id;
}