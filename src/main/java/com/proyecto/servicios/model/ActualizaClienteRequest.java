package com.proyecto.servicios.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActualizaClienteRequest {

    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")
    @Size(min = 2, max = 50)
    private String nombre;

    private String segundoNombre;

    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")
    private String apellidoPaterno;

    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")
    private String apellidoMaterno;

    private String nacionalidad;
    private String estadoCivil;

    @Email
    private String correoElectronico;

    @Pattern(regexp = "^\\d{10}$|^$")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$|^$")
    private String telefonoAlternativo;

    private String ocupacion;
    private String empresa;

    @DecimalMin(value = "0.01")
    private Double ingresoMensual;

    // Domicilio
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;

    @Pattern(regexp = "^\\d{5}$|^$")
    private String codigoPostal;

    private String pais;
}