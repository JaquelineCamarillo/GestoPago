package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;

import java.util.List;

public interface GestoPagoProductService {

    /**
     * Llama a GestoPago (getProductList.do) y guarda el catalogo. Prioriza Redis; si no
     * esta disponible, usa la BD local (Postgres) como respaldo.
     */
    void sincronizarCatalogo();

    /**
     * Lista el catalogo (Redis primero, BD local como respaldo).
     * @param tipoFront si viene null, regresa todo el catalogo. Si viene con un valor
     *                  (1, 2, 4, 5, 30, 31...), regresa solo los productos de ese tipo.
     *                  Los productos sin tipoFront (null) se normalizan a 0 al guardarse.
     */
    List<GestoPagoProductoResponse> listarProductosDisponibles(Integer tipoFront);
}