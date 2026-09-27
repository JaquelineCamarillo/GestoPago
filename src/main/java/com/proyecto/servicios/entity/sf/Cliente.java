package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 50)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", nullable = false, length = 18, unique = true)
    private String curp;

    @Column(name = "rfc", nullable = false, length = 13, unique = true)
    private String rfc;

    @Column(name = "sexo", nullable = false, length = 1)
    private String sexo;

    @Column(name = "nacionalidad", nullable = false, length = 50)
    private String nacionalidad;

    @Column(name = "estado_civil", nullable = false, length = 20)
    private String estadoCivil;

    @Column(name = "correo_electronico", nullable = false, length = 100, unique = true)
    private String correoElectronico;

    @Column(name = "telefono_movil", nullable = false, length = 10)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", length = 10)
    private String telefonoAlternativo;

    @Column(name = "ocupacion", length = 50)
    private String ocupacion;

    @Column(name = "empresa", length = 100)
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false)
    private Double ingresoMensual;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        fechaRegistro = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (activo == null) activo = true;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}