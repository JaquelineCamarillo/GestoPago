package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Criterio para consultar una cuenta o su saldo: el numero de cuenta viaja en el cuerpo. */
@Getter
@Setter
@NoArgsConstructor
public class CuentaConsultaRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{6,30}$",
            message = "El numero de cuenta debe tener entre 6 y 30 caracteres, solo letras y numeros, sin espacios")
    @Schema(example = "7654321A1B2C")
    private String numeroCuenta;
}