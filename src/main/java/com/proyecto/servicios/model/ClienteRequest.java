package com.proyecto.servicios.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]*$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$", message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(
            regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$",
            message = "La CURP no tiene un formato valido (18 caracteres)"
    )
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{2,3}$",
            message = "El RFC no tiene un formato valido (12 o 13 caracteres)"
    )
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[HM]$", message = "El sexo debe ser 'H' o 'M'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El correo electronico no tiene un formato valido")
    @Size(max = 100)
    private String correoElectronico;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$|^$", message = "El telefono alternativo debe contener exactamente 10 digitos")
    private String telefonoAlternativo;

    private String ocupacion;

    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private Double ingresoMensual;

    // ----- Domicilio -----
    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    private String numeroExterior;

    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El codigo postal debe contener exactamente 5 digitos")
    private String codigoPostal;

    @NotBlank(message = "El pais es obligatorio")
    private String pais;
}