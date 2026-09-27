package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    Optional<Cuenta> findByClienteId(Integer clienteId);
    List<Cuenta> findByEstatus(String estatus);
}