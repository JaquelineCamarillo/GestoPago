package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Piezas reutilizables para armar la consulta dinamica de clientes: cada metodo es un filtro
 * independiente y el servicio combina con "y" solo los que vienen en la peticion.
 */
public final class ClienteSpecs {

    private static final String CON_ACENTOS = "áéíóúüñÁÉÍÓÚÜÑ";
    private static final String SIN_ACENTOS = "aeiouunaeiouun";

    private ClienteSpecs() {
    }

    /** Punto de partida: no filtra nada. */
    public static Specification<Cliente> todos() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Cliente> conId(Integer id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    public static Specification<Cliente> conCurp(String curp) {
        return (root, query, cb) -> cb.equal(cb.upper(root.<String>get("curp")), curp.toUpperCase(Locale.ROOT));
    }

    public static Specification<Cliente> conRfc(String rfc) {
        return (root, query, cb) -> cb.equal(cb.upper(root.<String>get("rfc")), rfc.toUpperCase(Locale.ROOT));
    }

    public static Specification<Cliente> conCorreo(String correo) {
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("correoElectronico")), correo.toLowerCase(Locale.ROOT));
    }

    public static Specification<Cliente> conSexo(String sexo) {
        return (root, query, cb) -> cb.equal(root.get("sexo"), sexo);
    }

    public static Specification<Cliente> conNacionalidad(String codigoPais) {
        return (root, query, cb) -> cb.equal(cb.upper(root.<String>get("nacionalidad")), codigoPais.toUpperCase(Locale.ROOT));
    }

    public static Specification<Cliente> conActivo(Boolean activo) {
        return (root, query, cb) -> cb.equal(root.get("activo"), activo);
    }

    /** El cliente tiene una cuenta con ese numero (la cuenta vive en otra tabla). */
    public static Specification<Cliente> conNumeroCuenta(String numeroCuenta) {
        return (root, query, cb) -> {
            Subquery<Integer> subconsulta = query.subquery(Integer.class);
            Root<Cuenta> cuenta = subconsulta.from(Cuenta.class);
            subconsulta.select(cuenta.<Integer>get("id"))
                    .where(cb.equal(cuenta.get("cliente"), root),
                            cb.equal(cb.upper(cuenta.<String>get("numeroCuenta")), numeroCuenta.toUpperCase(Locale.ROOT)));
            return cb.exists(subconsulta);
        };
    }

    public static Specification<Cliente> registradoDesde(LocalDate desde) {
        LocalDateTime inicio = desde.atStartOfDay();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaRegistro"), inicio);
    }

    public static Specification<Cliente> registradoHasta(LocalDate hasta) {
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDateTime>get("fechaRegistro"), fin);
    }

    /**
     * La palabra (ya sin acentos y en minusculas) aparece en el nombre completo del cliente
     * (nombre, segundo nombre y apellidos), sin importar mayusculas ni acentos.
     */
    public static Specification<Cliente> nombreContiene(String palabra) {
        return (root, query, cb) -> cb.like(nombreCompletoSinAcentos(root, cb), "%" + palabra + "%");
    }

    private static Expression<String> nombreCompletoSinAcentos(Root<Cliente> root, CriteriaBuilder cb) {
        Expression<String> segundoNombre = cb.coalesce(root.<String>get("segundoNombre"), "");
        Expression<String> completo = cb.concat(
                cb.concat(cb.concat(root.<String>get("nombre"), " "), cb.concat(segundoNombre, " ")),
                cb.concat(cb.concat(root.<String>get("apellidoPaterno"), " "), root.<String>get("apellidoMaterno")));
        return cb.function("translate", String.class, cb.lower(completo),
                cb.literal(CON_ACENTOS), cb.literal(SIN_ACENTOS));
    }
}