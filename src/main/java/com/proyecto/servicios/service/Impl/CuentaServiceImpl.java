package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.clientes.CuentaNoEncontradaException;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaServiceImpl(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        return aResponse(buscar(numeroCuenta));
    }

    @Override
    public List<CuentaResponse> obtenerActivas() {
        return cuentaRepository.findByEstatus("ACTIVA").stream()
                .map(this::aResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double consultarSaldo(String numeroCuenta) {
        return buscar(numeroCuenta).getSaldo();
    }

    private Cuenta buscar(String numeroCuenta) {
        // El numero de cuenta se guarda en mayusculas: se normaliza lo que escribe el cliente
        String numero = numeroCuenta == null ? "" : numeroCuenta.trim().toUpperCase(Locale.ROOT);
        return cuentaRepository.findByNumeroCuenta(numero)
                .orElseThrow(() -> new CuentaNoEncontradaException(numero));
    }

    private CuentaResponse aResponse(Cuenta cuenta) {
        return new CuentaResponse(
                cuenta.getNumeroCuenta(), cuenta.getSaldo(), cuenta.getEstatus(),
                cuenta.getCliente().getId());
    }
}