package com.proyecto.servicios.model;

import com.proyecto.servicios.validation.NumeroCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos para abonar saldo a una cuenta activa (POST /cuentas/abonar).
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaAbonoRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @NumeroCuenta
    @Schema(example = "7904852077759512")
    private String numeroCuenta;

    @NotNull(message = "El monto a abonar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a abonar debe ser mayor a 0")
    @DecimalMax(value = "999999.99", message = "El monto a abonar no puede exceder 999999.99")
    @Digits(integer = 6, fraction = 2, message = "El monto a abonar debe tener como maximo 6 digitos enteros y 2 decimales")
    @Schema(example = "1000.00")
    private BigDecimal monto;
}
