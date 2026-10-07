package com.proyecto.servicios.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Catalogo cerrado de generos permitidos al registrar un cliente.
 * En base de datos se guarda el "codigo" (texto corto), no el nombre del enum.
 */
public enum Genero {

    HOMBRE("H", "Hombre"),
    MUJER("M", "Mujer"),
    NO_BINARIO("X", "No binario"),
    OTRO("O", "Otro"),
    PREFIERO_NO_DECIR("N", "Prefiero no decirlo");

    private final String codigo;
    private final String etiqueta;

    Genero(String codigo, String etiqueta) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Busca por codigo ("H", "M", "X"...) ignorando mayusculas. */
    public static Optional<Genero> desdeCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(g -> g.codigo.equalsIgnoreCase(codigo.trim()))
                .findFirst();
    }

    /** Codigos validos, para armar mensajes de error claros. */
    public static List<String> codigosValidos() {
        return Arrays.stream(values()).map(Genero::getCodigo).toList();
    }
}