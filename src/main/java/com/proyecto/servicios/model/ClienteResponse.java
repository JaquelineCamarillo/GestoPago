package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {
    private Integer id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correoElectronico;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private Double ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private String numeroCuenta;
    private Double saldo;
    private String estatusCuenta;
}