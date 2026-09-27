package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login")
@Getter
@Setter
public class Login {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "password_cifrado", nullable = false, columnDefinition = "TEXT")
    private String passwordCifrado;

    @Column(name = "token_cifrado", columnDefinition = "TEXT")
    private String tokenCifrado;

    @Column(name = "embedding_facial_cifrado", columnDefinition = "TEXT")
    private String embeddingFacialCifrado;

    @Column(name = "activo", nullable = false)
    private Boolean activo = false;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;
}