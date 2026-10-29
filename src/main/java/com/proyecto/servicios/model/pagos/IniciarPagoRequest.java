package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.enums.TipoPago;
import com.proyecto.servicios.validation.NumeroCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos para iniciar una solicitud de pago o recarga (POST /pagos/iniciar).
 * Incluye la primera verificacion (tokenSesion del login) y valida las reglas por tipoPago.
 */
@Getter
@Setter
@NoArgsConstructor
public class IniciarPagoRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @NumeroCuenta
    @Schema(example = "7904852077759512", description = "Numero de cuenta de exactamente 16 digitos numericos")
    private String numeroCuenta;

    @NotNull(message = "El idUser (id del cliente titular) es obligatorio")
    @Positive(message = "El idUser debe ser un numero entero positivo")
    @Schema(example = "1", description = "Identificador del cliente titular de la cuenta")
    private Integer idUser;

    @NotBlank(message = "El tokenSesion es obligatorio")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "El tokenSesion debe tener formato UUID valido")
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Token de sesion activa del login (1a verificacion)")
    private String tokenSesion;

    @NotNull(message = "El tipoPago es obligatorio (RECARGA o SERVICIO)")
    @Schema(example = "RECARGA", description = "Tipo de operacion: RECARGA o SERVICIO")
    private TipoPago tipoPago;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser un numero entero positivo")
    @Schema(example = "71", description = "Identificador del servicio en el catalogo de GestoPago")
    private Integer idServicio;

    @NotNull(message = "El idProd es obligatorio")
    @Positive(message = "El idProd debe ser un numero entero positivo")
    @Schema(example = "200", description = "Identificador del producto en el catalogo de GestoPago")
    private Integer idProd;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @DecimalMax(value = "999999.99", message = "El monto no puede exceder 999999.99")
    @Digits(integer = 6, fraction = 2, message = "El monto debe tener como maximo 6 digitos enteros y 2 decimales")
    @Schema(example = "100.00", description = "Monto de la operacion (positivo, hasta 999999.99 y maximo 2 decimales)")
    private BigDecimal monto;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono debe tener exactamente 10 digitos numericos, sin espacios ni letras")
    @Schema(example = "5555555551", description = "Telefono destino de 10 digitos (obligatorio en RECARGA; no permitido en SERVICIO)")
    private String telefono;

    @Pattern(regexp = "^[A-Za-z0-9]{1,30}$", message = "La referencia debe ser alfanumerica de entre 1 y 30 caracteres, sin espacios")
    @Schema(example = "25720381", description = "Referencia del recibo (obligatoria en SERVICIO; no permitida en RECARGA)")
    private String referencia;

    @JsonIgnore
    @AssertTrue(message = "Cuando tipoPago es RECARGA, el telefono de 10 digitos es obligatorio y no debe enviarse referencia")
    public boolean isDestinoRecargaValido() {
        if (tipoPago != TipoPago.RECARGA) {
            return true;
        }
        boolean tieneTelefono = telefono != null && !telefono.isBlank();
        boolean tieneReferencia = referencia != null && !referencia.isBlank();
        return tieneTelefono && !tieneReferencia;
    }

    @JsonIgnore
    @AssertTrue(message = "Cuando tipoPago es SERVICIO, la referencia (1 a 30 caracteres alfanumericos) es obligatoria y no debe enviarse telefono")
    public boolean isDestinoServicioValido() {
        if (tipoPago != TipoPago.SERVICIO) {
            return true;
        }
        boolean tieneReferencia = referencia != null && !referencia.isBlank();
        boolean tieneTelefono = telefono != null && !telefono.isBlank();
        return tieneReferencia && !tieneTelefono;
    }
}
