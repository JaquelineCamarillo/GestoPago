package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.util.CurpValidador;
import com.proyecto.servicios.util.RfcValidador;
import com.proyecto.servicios.validation.CodigoPais;
import com.proyecto.servicios.validation.Curp;
import com.proyecto.servicios.validation.Rfc;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    // Letras (con acentos, dieresis y enie). Se escribe con escapes unicode para no depender de la codificacion.
    private static final String LETRAS =
            "A-Za-z\u00C1\u00C9\u00CD\u00D3\u00DA\u00DC\u00D1\u00E1\u00E9\u00ED\u00F3\u00FA\u00FC\u00F1";
    private static final String SOLO_LETRAS = "^([" + LETRAS + "]+( [" + LETRAS + "]+)*)?$";
    private static final String TEXTO_DIRECCION = "^[" + LETRAS + "0-9 .,#/\u00B0'&-]*$";

    private static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final DateTimeFormatter FORMATO_RFC = DateTimeFormatter.ofPattern("yyMMdd");

    // ----- Datos personales -----

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El nombre solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    @Schema(example = "Juana")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede tener mas de 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El segundo nombre solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    @Schema(example = "Jaqueline")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El apellido paterno solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    @Schema(example = "Camarillo")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El apellido materno solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    @Schema(example = "Olaez")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria (formato yyyy/MM/dd, ejemplo: 1995/05/18)")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu/MM/dd", lenient = OptBoolean.FALSE)
    @Schema(type = "string", example = "1995/05/18", description = "Formato yyyy/MM/dd")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Curp
    @Schema(example = "CAOJ950518MGTMLQ08")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Rfc
    @Schema(example = "CAOJ950518AB9")
    private String rfc;

    @NotBlank(message = "El genero es obligatorio")
    @Pattern(regexp = "^[HMXON]$",
            message = "El genero debe ser H (Hombre), M (Mujer), X (No binario), O (Otro) o N (Prefiero no decirlo). "
                    + "Consulte GET /catalogos/generos")
    @Schema(example = "M", description = "Codigo del catalogo GET /catalogos/generos")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @CodigoPais
    @Schema(example = "MX", description = "Codigo del catalogo GET /catalogos/nacionalidades")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Size(max = 20, message = "El estado civil no puede tener mas de 20 caracteres")
    @Pattern(regexp = "^(?i)(soltero|soltera|casado|casada|divorciado|divorciada|viudo|viuda|union libre|separado|separada)$",
            message = "El estado civil debe ser: Soltero(a), Casado(a), Divorciado(a), Viudo(a), Union libre o Separado(a)")
    @Schema(example = "Soltera")
    private String estadoCivil;

    // ----- Contacto -----

    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 100, message = "El correo electronico no puede tener mas de 100 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "El correo electronico no tiene un formato valido (ejemplo: nombre@dominio.com)")
    @Schema(example = "juana@example.com")
    private String correoElectronico;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$|^$",
            message = "El telefono movil debe contener exactamente 10 digitos, solo numeros")
    @Schema(example = "4771234567")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$|^$",
            message = "El telefono alternativo debe contener exactamente 10 digitos, solo numeros")
    @Schema(example = "4779876543")
    private String telefonoAlternativo;

    // ----- Datos laborales -----

    @Size(max = 50, message = "La ocupacion no puede tener mas de 50 caracteres")
    @Pattern(regexp = "^[" + LETRAS + " ./-]*$",
            message = "La ocupacion solo debe contener letras (sin numeros ni simbolos)")
    @Schema(example = "Ingeniera")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede tener mas de 100 caracteres")
    @Pattern(regexp = TEXTO_DIRECCION,
            message = "La empresa solo puede contener letras, numeros y los simbolos . , # / ' & -")
    @Schema(example = "LKSBASS")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio y debe ser un numero")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @DecimalMax(value = "9999999999.99", message = "El ingreso mensual es demasiado grande")
    @Schema(example = "15000.50")
    private Double ingresoMensual;

    // ----- Domicilio -----

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede tener mas de 100 caracteres")
    @Pattern(regexp = TEXTO_DIRECCION,
            message = "La calle solo puede contener letras, numeros y los simbolos . , # / ' & -")
    @Schema(example = "Reforma")
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    @Size(max = 10, message = "El numero exterior no puede tener mas de 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9/-]*$",
            message = "El numero exterior solo puede contener letras, numeros, guion o diagonal (ejemplo: 100, 12-A, S/N)")
    @Schema(example = "100")
    private String numeroExterior;

    @Size(max = 10, message = "El numero interior no puede tener mas de 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9/-]*$",
            message = "El numero interior solo puede contener letras, numeros, guion o diagonal (ejemplo: 4, B-2)")
    @Schema(example = "4B")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no puede tener mas de 100 caracteres")
    @Pattern(regexp = TEXTO_DIRECCION,
            message = "La colonia solo puede contener letras, numeros y los simbolos . , # / ' & -")
    @Schema(example = "Centro")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no puede tener mas de 100 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El municipio solo debe contener letras (sin numeros ni simbolos)")
    @Schema(example = "Leon")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no puede tener mas de 100 caracteres")
    @Pattern(regexp = SOLO_LETRAS,
            message = "El estado solo debe contener letras (sin numeros ni simbolos)")
    @Schema(example = "Guanajuato")
    private String estado;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$|^$",
            message = "El codigo postal debe contener exactamente 5 digitos, solo numeros")
    @Schema(example = "37000")
    private String codigoPostal;

    @NotBlank(message = "El pais es obligatorio")
    @CodigoPais
    @Schema(example = "MX", description = "Codigo del catalogo GET /catalogos/nacionalidades")
    private String pais;

    // ================= Validaciones cruzadas entre campos =================
    // Solo se evaluan cuando los campos involucrados son validos por si solos;
    // si no, ya se reporta el error de cada campo y estas devuelven true.

    @JsonIgnore
    @AssertTrue(message = "La fecha de nacimiento debe ser posterior al 1900/01/01")
    public boolean isFechaNacimientoRazonable() {
        return fechaNacimiento == null || !fechaNacimiento.isBefore(FECHA_MINIMA);
    }

    @JsonIgnore
    @AssertTrue(message = "El ingreso mensual solo puede tener hasta 2 decimales")
    public boolean isIngresoMensualConDosDecimales() {
        return ingresoMensual == null
                || BigDecimal.valueOf(ingresoMensual).stripTrailingZeros().scale() <= 2;
    }

    @JsonIgnore
    @AssertTrue(message = "La fecha de nacimiento no coincide con la que contiene la CURP")
    public boolean isFechaNacimientoCoincideConCurp() {
        if (fechaNacimiento == null || !CurpValidador.esValida(curp)) {
            return true;
        }
        return CurpValidador.fechaNacimiento(curp).equals(fechaNacimiento);
    }

    @JsonIgnore
    @AssertTrue(message = "La fecha de nacimiento no coincide con la que contiene el RFC")
    public boolean isFechaNacimientoCoincideConRfc() {
        if (fechaNacimiento == null || !RfcValidador.esValido(rfc)) {
            return true;
        }
        return RfcValidador.normalizar(rfc).substring(4, 10).equals(fechaNacimiento.format(FORMATO_RFC));
    }

    @JsonIgnore
    @AssertTrue(message = "El sexo no coincide con la posicion 11 de la CURP (H, M o X)")
    public boolean isSexoCoincideConCurp() {
        if (sexo == null || !CurpValidador.esValida(curp)) {
            return true;
        }
        String codigo = sexo.trim().toUpperCase();
        if (codigo.equals("O") || codigo.equals("N")) {
            return true; // la CURP solo distingue H, M y X
        }
        if (!codigo.equals("H") && !codigo.equals("M") && !codigo.equals("X")) {
            return true; // el error de formato ya lo reporta el @Pattern
        }
        return codigo.charAt(0) == CurpValidador.sexo(curp);
    }

    @JsonIgnore
    @AssertTrue(message = "Las 4 primeras letras del RFC no coinciden con las de la CURP")
    public boolean isRfcCoincideConCurp() {
        if (!CurpValidador.esValida(curp) || !RfcValidador.esValido(rfc)) {
            return true;
        }
        return RfcValidador.normalizar(rfc).substring(0, 4)
                .equals(CurpValidador.normalizar(curp).substring(0, 4));
    }
}