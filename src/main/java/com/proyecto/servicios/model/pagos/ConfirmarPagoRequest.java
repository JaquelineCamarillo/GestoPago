package com.proyecto.servicios.model.pagos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para confirmar y ejecutar un pago previamente iniciado (POST /pagos/confirmar).
 * Incluye el codigo de 6 digitos (segunda verificacion: "soy yo quien paga").
 */
@Getter
@Setter
@NoArgsConstructor
public class ConfirmarPagoRequest {

    @NotNull(message = "El pagoId es obligatorio")
    @Positive(message = "El pagoId debe ser un numero entero positivo")
    @Schema(example = "1", description = "Identificador del pago devuelto por /pagos/iniciar")
    private Integer pagoId;

    @NotBlank(message = "El tokenSesion es obligatorio")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "El tokenSesion debe tener formato UUID valido")
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Token de sesion activa del cliente")
    private String tokenSesion;

    @NotBlank(message = "El codigo de confirmacion es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El codigo de confirmacion debe tener exactamente 6 digitos numericos")
    @Schema(example = "123456", description = "Codigo de segunda verificacion de 6 digitos")
    private String codigo;
}
