package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaResponse {
    private String numeroCuenta;
    private Double saldo;
    private String estatus;
    private Integer clienteId;
}