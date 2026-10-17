package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.proyecto.servicios.validation.CodigoPais;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * El id identifica al cliente; el resto de los campos son opcionales: solo se modifican los que vienen en la peticion.
 * Los campos que vienen deben cumplir las mismas reglas que al crear el cliente.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaClienteRequest {

    // Letras (con acentos, dieresis y enie), con escapes unicode para no depender de la codificacion.
    private static final String LETRAS =
            "A-Za-z\u00C1\u00C9\u00CD\u00D3\u00DA\u00DC\u00D1\u00E1\u00E9\u00ED\u00F3\u00FA\u00FC\u00F1";
    /** Letras separadas por un espacio; si viene, no puede estar vacio. */
    private static final String SOLO_LETRAS = "^[" + LETRAS + "]+( [" + LETRAS + "]+)*$";
    /** Igual que SOLO_LETRAS pero permite vaciar el campo (campos opcionales). */
    private static final String SOLO_LETRAS_O_VACIO = "^([" + LETRAS + "]+( [" + LETRAS + "]+)*)?$";
    /** Texto de direccion: al menos una letra o numero. */
    private static final String TEXTO_DIRECCION =
            "^(?=.*[" + LETRAS + "0-9])[" + LETRAS + "0-9 .,#/\u00B0'&-]+$";
    private static final String TEXTO_DIRECCION_O_VACIO = "^[" + LETRAS + "0-9 .,#/\u00B0'&-]*$";


    // ----- Identificador del cliente (viaja en el cuerpo, no en la URL) -----

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero entero positivo")
    @Schema(example = "1", description = "Cliente a modificar")
    private Integer id;

    // ----- Datos personales -----

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El nombre solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    @Schema(example = "Juana")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede tener mas de 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS_O_VACIO,
            message = "El segundo nombre solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    private String segundoNombre;

    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El apellido paterno solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    private String apellidoPaterno;

    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El apellido materno solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    private String apellidoMaterno;

    @Pattern(regexp = "^[A-Za-z]{2}$",
            message = "La nacionalidad debe ser un codigo de 2 letras (ejemplo: MX). Consulte GET /catalogos/nacionalidades")
    @CodigoPais
    @Schema(example = "MX")
    private String nacionalidad;

    @Size(max = 20, message = "El estado civil no puede tener mas de 20 caracteres")
    @Pattern(regexp = "^(?i)(soltero|soltera|casado|casada|divorciado|divorciada|viudo|viuda|union libre|separado|separada)$",
            message = "El estado civil debe ser: Soltero(a), Casado(a), Divorciado(a), Viudo(a), Union libre o Separado(a)")
    private String estadoCivil;

    // ----- Contacto -----

    @Size(max = 100, message = "El correo electronico no puede tener mas de 100 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "El correo electronico no tiene un formato valido (ejemplo: nombre@dominio.com)")
    private String correoElectronico;

    @Pattern(regexp = "^\\d{10}$",
            message = "El telefono movil debe contener exactamente 10 digitos, solo numeros")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$|^$",
            message = "El telefono alternativo debe contener exactamente 10 digitos, solo numeros")
    private String telefonoAlternativo;

    // ----- Datos laborales -----

    @Size(max = 50, message = "La ocupacion no puede tener mas de 50 caracteres")
    @Pattern(regexp = "^[" + LETRAS + " ./-]*$",
            message = "La ocupacion solo debe contener letras (sin numeros ni simbolos)")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede tener mas de 100 caracteres")
    @Pattern(regexp = TEXTO_DIRECCION_O_VACIO,
            message = "La empresa solo puede contener letras, numeros y los simbolos . , # / ' & -")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @DecimalMax(value = "9999999999.99", message = "El ingreso mensual es demasiado grande")
    @Schema(example = "18000.00")
    private Double ingresoMensual;

    // ----- Domicilio -----

    @Size(max = 100, message = "La calle no puede tener mas de 100 caracteres")
    @Pattern(regexp = TEXTO_DIRECCION,
            message = "La calle no puede estar vacia y solo puede contener letras, numeros y los simbolos . , # / ' & -")
    private String calle;

    @Size(max = 10, message = "El numero exterior no puede tener mas de 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9/-]+$",
            message = "El numero exterior no puede estar vacio y solo puede contener letras, numeros, guion o diagonal")
    private String numeroExterior;

    @Size(max = 10, message = "El numero interior no puede tener mas de 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9/-]*$",
            message = "El numero interior solo puede contener letras, numeros, guion o diagonal")
    private String numeroInterior;

    @Size(max = 100, message = "La colonia no puede tener mas de 100 caracteres")
    @Pattern(regexp = TEXTO_DIRECCION,
            message = "La colonia no puede estar vacia y solo puede contener letras, numeros y los simbolos . , # / ' & -")
    private String colonia;

    @Size(max = 100, message = "El municipio no puede tener mas de 100 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El municipio no puede estar vacio y solo debe contener letras (sin numeros ni simbolos)")
    private String municipio;

    @Size(max = 100, message = "El estado no puede tener mas de 100 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El estado no puede estar vacio y solo debe contener letras (sin numeros ni simbolos)")
    private String estado;

    @Pattern(regexp = "^\\d{5}$",
            message = "El codigo postal debe contener exactamente 5 digitos, solo numeros")
    private String codigoPostal;

    @Pattern(regexp = "^[A-Za-z]{2}$",
            message = "El pais debe ser un codigo de 2 letras (ejemplo: MX). Consulte GET /catalogos/nacionalidades")
    @CodigoPais
    @Schema(example = "MX")
    private String pais;

    // ----- Campos que NO se pueden modificar -----
    // Si llegan en la peticion se rechazan con un mensaje claro (antes se ignoraban en silencio).

    @Null(message = "La CURP no se puede modificar")
    @Schema(description = "No modificable: debe omitirse")
    private String curp;

    @Null(message = "El RFC no se puede modificar")
    @Schema(description = "No modificable: debe omitirse")
    private String rfc;

    @Null(message = "El sexo no se puede modificar")
    @Schema(description = "No modificable: debe omitirse")
    private String sexo;

    @Null(message = "La fecha de nacimiento no se puede modificar")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu/MM/dd", lenient = OptBoolean.FALSE)
    @Schema(type = "string", description = "No modificable: debe omitirse")
    private LocalDate fechaNacimiento;

    @Null(message = "El numero de cuenta no se puede modificar")
    @Schema(description = "No modificable: debe omitirse")
    private String numeroCuenta;

    // ================= Validaciones cruzadas =================

    @JsonIgnore
    @AssertTrue(message = "Debe enviar al menos un campo para modificar")
    public boolean isTrayendoAlMenosUnCampo() {
        return nombre != null || segundoNombre != null || apellidoPaterno != null || apellidoMaterno != null
                || nacionalidad != null || estadoCivil != null || correoElectronico != null
                || telefonoMovil != null || telefonoAlternativo != null || ocupacion != null
                || empresa != null || ingresoMensual != null || calle != null || numeroExterior != null
                || numeroInterior != null || colonia != null || municipio != null || estado != null
                || codigoPostal != null || pais != null
                // si solo vienen campos no modificables, se reportan ellos y no este mensaje
                || curp != null || rfc != null || sexo != null || fechaNacimiento != null || numeroCuenta != null;
    }

    @JsonIgnore
    @AssertTrue(message = "El ingreso mensual solo puede tener hasta 2 decimales")
    public boolean isIngresoMensualConDosDecimales() {
        return ingresoMensual == null
                || BigDecimal.valueOf(ingresoMensual).stripTrailingZeros().scale() <= 2;
    }
}