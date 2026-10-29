package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.proyecto.servicios.validation.CodigoPais;
import com.proyecto.servicios.validation.Curp;
import com.proyecto.servicios.validation.NumeroCuenta;
import com.proyecto.servicios.validation.Rfc;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Criterios de busqueda de clientes. Todos son opcionales, pero debe venir al menos uno.
 * Los criterios que se envian se combinan con "y" (el cliente debe cumplirlos todos).
 */
@Getter
@Setter
@NoArgsConstructor
public class BuscarClienteRequest {

    // Letras (con acentos, dieresis y enie), con escapes unicode para no depender de la codificacion.
    private static final String LETRAS =
            "A-Za-zÁÉÍÓÚÜÑáéíóúüñ";

    @Positive(message = "El id debe ser un numero entero positivo")
    @Schema(example = "1")
    private Integer id;

    @Size(min = 2, max = 100, message = "El nombre a buscar debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[" + LETRAS + "]+( [" + LETRAS + "]+)*$",
            message = "El nombre a buscar solo debe contener letras (sin numeros ni simbolos) y un espacio entre palabras")
    @Schema(example = "juana camarillo",
            description = "Una o varias palabras del nombre completo; no distingue mayusculas ni acentos")
    private String nombre;

    @Curp
    @Schema(example = "CAOJ950518MGTMLQ08")
    private String curp;

    @Rfc
    @Schema(example = "CAOJ950518AB9")
    private String rfc;

    @Size(max = 100, message = "El correo a buscar no puede tener mas de 100 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "El correo a buscar no tiene un formato valido (ejemplo: nombre@dominio.com)")
    @Schema(example = "juana@example.com")
    private String correoElectronico;

    @NumeroCuenta
    @Schema(example = "7904852077759512")
    private String numeroCuenta;

    @Pattern(regexp = "^[HMXON]$",
            message = "El genero debe ser H (Hombre), M (Mujer), X (No binario), O (Otro) o N (Prefiero no decirlo). "
                    + "Consulte /catalogos/generos")
    @Schema(example = "M")
    private String sexo;

    @Pattern(regexp = "^[A-Za-z]{2}$",
            message = "La nacionalidad debe ser un codigo de 2 letras (ejemplo: MX). Consulte /catalogos/nacionalidades")
    @CodigoPais
    @Schema(example = "MX")
    private String nacionalidad;

    @Schema(example = "true", description = "true = solo clientes activos; false = solo clientes dados de baja")
    private Boolean activo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu/MM/dd", lenient = OptBoolean.FALSE)
    @Schema(type = "string", example = "2026/01/01", description = "Registrado desde (inclusive). Formato yyyy/MM/dd")
    private LocalDate fechaRegistroDesde;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu/MM/dd", lenient = OptBoolean.FALSE)
    @Schema(type = "string", example = "2026/12/31", description = "Registrado hasta (inclusive). Formato yyyy/MM/dd")
    private LocalDate fechaRegistroHasta;

    // ================= Validaciones cruzadas =================

    @JsonIgnore
    @AssertTrue(message = "Debe enviar al menos un criterio de busqueda: id, nombre, curp, rfc, correoElectronico, "
            + "numeroCuenta, sexo, nacionalidad, activo, fechaRegistroDesde o fechaRegistroHasta")
    public boolean isTrayendoAlMenosUnCriterio() {
        return id != null || nombre != null || curp != null || rfc != null || correoElectronico != null
                || numeroCuenta != null || sexo != null || nacionalidad != null || activo != null
                || fechaRegistroDesde != null || fechaRegistroHasta != null;
    }

    @JsonIgnore
    @AssertTrue(message = "La fecha 'fechaRegistroDesde' no puede ser posterior a 'fechaRegistroHasta'")
    public boolean isRangoDeFechasValido() {
        return fechaRegistroDesde == null || fechaRegistroHasta == null
                || !fechaRegistroDesde.isAfter(fechaRegistroHasta);
    }
}