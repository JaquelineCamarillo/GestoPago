package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistrarCredencialesRequest {
    @NotNull(message = "El id del cliente es obligatorio")
    private Integer clienteId;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    /** Vector de embedding facial (generado en el cliente, ej. MediaPipe/face-api.js), numeros separados por coma. Opcional. */
    private String embeddingFacial;
}