package com.proyecto.servicios.model;

/**
 * Elemento generico de un catalogo: un codigo (lo que se guarda y se envia
 * en las peticiones) y una etiqueta (lo que ve la persona usuaria).
 */
public record CatalogoItem(String codigo, String etiqueta) {
}