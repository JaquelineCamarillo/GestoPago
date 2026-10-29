package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Pago;
import com.proyecto.servicios.enums.EstadoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findByTrace(String trace);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pago p WHERE p.id = :id")
    Optional<Pago> findByIdForUpdate(@Param("id") Integer id);

    /**
     * Detecta si existe una solicitud identica (misma cuenta, servicio, producto, destino y monto)
     * dentro de una ventana de tiempo reciente en alguno de los estados indicados.
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM Pago p
            WHERE p.cuenta.id = :cuentaId
              AND p.idServicio = :idServicio
              AND p.idProducto = :idProducto
              AND p.monto = :monto
              AND ((:telefono IS NULL AND p.telefono IS NULL) OR p.telefono = :telefono)
              AND ((:referencia IS NULL AND p.referencia IS NULL) OR p.referencia = :referencia)
              AND p.estado IN :estados
              AND p.fechaCreacion >= :desde
            """)
    boolean existeSolicitudDuplicadaEnVentana(
            @Param("cuentaId") Integer cuentaId,
            @Param("idServicio") Integer idServicio,
            @Param("idProducto") Integer idProducto,
            @Param("monto") BigDecimal monto,
            @Param("telefono") String telefono,
            @Param("referencia") String referencia,
            @Param("estados") Collection<EstadoPago> estados,
            @Param("desde") LocalDateTime desde
    );

    List<Pago> findByEstadoOrderByFechaCreacionAsc(EstadoPago estado);
}
