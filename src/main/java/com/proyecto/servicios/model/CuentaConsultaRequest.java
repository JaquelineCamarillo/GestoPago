package com.proyecto.servicios.model;

import com.proyecto.servicios.validation.NumeroCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Criterio para consultar una cuenta o su saldo: el numero de cuenta viaja en el cuerpo. */
@Getter
@Setter
@NoArgsConstructor
public class CuentaConsultaRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @NumeroCuenta
    @Schema(example = "7904852077759512")
    private String numeroCuenta;
}