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
 * Datos para consultar el estado y bitacora de un pago o solicitar su conciliacion (POST /pagos/consultar y POST /pagos/conciliar).
 */
@Getter
@Setter
@NoArgsConstructor
public class ConsultarPagoRequest {

    @NotNull(message = "El pagoId es obligatorio")
    @Positive(message = "El pagoId debe ser un numero entero positivo")
    @Schema(example = "1", description = "Identificador del pago")
    private Integer pagoId;

    @NotBlank(message = "El tokenSesion es obligatorio")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "El tokenSesion debe tener formato UUID valido")
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Token de sesion activa del cliente")
    private String tokenSesion;
}
