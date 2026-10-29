package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 20)
    private String numeroCuenta;

    @Column(name = "saldo", nullable = false)
    private Double saldo = 0.0;

    @Column(name = "saldo_retenido", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoRetenido = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "estatus", nullable = false, length = 10)
    private String estatus = "ACTIVA";

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDateTime fechaApertura;

    /** Saldo disponible exacto con 2 decimales (saldo_actual - saldo_retenido). */
    public BigDecimal getSaldoDisponible() {
        BigDecimal actual = saldo == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(saldo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal retenido = saldoRetenido == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : saldoRetenido.setScale(2, RoundingMode.HALF_UP);
        return actual;
    }

    @PrePersist
    void onCreate() {
        fechaApertura = LocalDateTime.now();
        if (saldoRetenido == null) {
            saldoRetenido = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
