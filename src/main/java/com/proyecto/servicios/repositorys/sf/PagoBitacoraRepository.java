package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.PagoBitacora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoBitacoraRepository extends JpaRepository<PagoBitacora, Integer> {

    List<PagoBitacora> findByPagoIdOrderByFechaRegistroAscIdAsc(Integer pagoId);
}
