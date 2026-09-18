package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;

import java.util.List;

public interface GestoPagoProductService {

    /**
     * Llama a GestoPago (getProductList.do) y guarda/actualiza el catalogo en la BD local.
     * Segun la documentacion de GestoPago, este metodo solo puede llamarse hasta 3 veces
     * al dia o se bloquea la IP; por eso corre programado 1 vez al dia.
     */
    void sincronizarCatalogo();

    /** Lee el catalogo desde la BD local. Nunca llama a GestoPago en vivo. */
    List<GestoPagoProductoResponse> listarProductosDisponibles();
}