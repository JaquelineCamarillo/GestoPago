package com.proyecto.servicios.entity.sf;

import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.enums.TipoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "numero_cuenta", nullable = false, length = 16)
    private String numeroCuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 20)
    private TipoPago tipoPago;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "nombre_servicio", length = 256)
    private String nombreServicio;

    @Column(name = "nombre_producto", length = 256)
    private String nombreProducto;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "trace", nullable = false, unique = true, length = 15)
    private String trace;

    @Column(name = "hora_local", nullable = false, length = 15)
    private String horaLocal;

    @Column(name = "unidad", nullable = false, length = 25)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPago estado;

    @Column(name = "codigo_cifrado", nullable = false, columnDefinition = "TEXT")
    private String codigoCifrado;

    @Column(name = "codigo_expiracion", nullable = false)
    private LocalDateTime codigoExpiracion;

    @Column(name = "intentos_codigo", nullable = false)
    private Integer intentosCodigo = 0;

    @Column(name = "monto_retenido", nullable = false)
    private Boolean montoRetenido = false;

    @Column(name = "id_tx_proveedor", length = 60)
    private String idTxProveedor;

    @Column(name = "num_autorizacion", length = 60)
    private String numAutorizacion;

    @Column(name = "codigo_proveedor", length = 20)
    private String codigoProveedor;

    @Column(name = "mensaje_proveedor", length = 500)
    private String mensajeProveedor;

    @Column(name = "pin_proveedor", length = 250)
    private String pinProveedor;

    @Column(name = "leyenda_proveedor", columnDefinition = "TEXT")
    private String leyendaProveedor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
        if (intentosCodigo == null) {
            intentosCodigo = 0;
        }
        if (montoRetenido == null) {
            montoRetenido = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
