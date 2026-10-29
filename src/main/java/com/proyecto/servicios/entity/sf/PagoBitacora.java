package com.proyecto.servicios.entity.sf;

import com.proyecto.servicios.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_bitacora")
@Getter
@Setter
@NoArgsConstructor
public class PagoBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoPago estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoPago estadoNuevo;

    @Column(name = "detalle", nullable = false, length = 500)
    private String detalle;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public PagoBitacora(Pago pago, EstadoPago estadoAnterior, EstadoPago estadoNuevo, String detalle) {
        this.pago = pago;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.detalle = detalle;
        this.fechaRegistro = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
