package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CuentaResponse;

import java.util.List;

public interface CuentaService {
    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta);
    List<CuentaResponse> obtenerActivas();
    Double consultarSaldo(String numeroCuenta);
}