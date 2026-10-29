package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.clientes.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.clientes.ValidacionNegocioException;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Override
    @Transactional
    public CuentaResponse abonarSaldo(String numeroCuenta, BigDecimal monto) {
        Cuenta cuenta = buscar(numeroCuenta);
        Cuenta bloqueada = cuentaRepository.findByIdForUpdate(cuenta.getId()).orElse(cuenta);
        if (!"ACTIVA".equalsIgnoreCase(bloqueada.getEstatus())) {
            throw new ValidacionNegocioException("La cuenta " + bloqueada.getNumeroCuenta() + " no se encuentra ACTIVA");
        }
        BigDecimal saldoActual = bloqueada.getSaldo() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(bloqueada.getSaldo()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevoSaldo = saldoActual.add(monto.setScale(2, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
        bloqueada.setSaldo(nuevoSaldo.doubleValue());
        cuentaRepository.save(bloqueada);
        return aResponse(bloqueada);
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