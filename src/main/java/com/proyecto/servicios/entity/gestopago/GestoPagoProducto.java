package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "gestopago_productos")
@Getter
@Setter
public class GestoPagoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "nombre_servicio", length = 256)
    private String nombreServicio;

    @Column(name = "nombre_producto", length = 256)
    private String nombreProducto;

    @Column(name = "precio", length = 20)
    private String precio;

    @Column(name = "tipo_referencia", length = 5)
    private String tipoReferencia;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        fechaActualizacion = LocalDateTime.now();
    }
}