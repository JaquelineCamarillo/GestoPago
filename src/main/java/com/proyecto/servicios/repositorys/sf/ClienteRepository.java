package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreoElectronico(String correoElectronico);

    /** Busca el correo sin distinguir mayusculas de minusculas. */
    Optional<Cliente> findByCorreoElectronicoIgnoreCase(String correoElectronico);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);

    /**
     * Clientes cuyo nombre, segundo nombre o apellidos contienen el texto (sin distinguir mayusculas).
     * El servicio afina el resultado cuando se escriben varias palabras.
     */
    @Query("select c from Cliente c where "
            + "lower(c.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(c.segundoNombre) like lower(concat('%', :texto, '%')) "
            + "or lower(c.apellidoPaterno) like lower(concat('%', :texto, '%')) "
            + "or lower(c.apellidoMaterno) like lower(concat('%', :texto, '%'))")
    List<Cliente> buscarPorTextoDeNombre(@Param("texto") String texto);
}