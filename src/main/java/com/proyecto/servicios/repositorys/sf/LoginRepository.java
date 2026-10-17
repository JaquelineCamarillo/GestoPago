package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Integer> {
    Optional<Login> findByClienteId(Integer clienteId);
    List<Login> findByActivoTrue();
}