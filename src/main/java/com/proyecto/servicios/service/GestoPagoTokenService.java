package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;

import java.util.Optional;

public interface GestoPagoTokenService {

    void renovarToken();

    Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo);

    /**
     * Obtiene un token Bearer valido para invocar servicios de GestoPago. Si no hay
     * token en cache o esta expirado, renueva de forma sincrona. A diferencia de
     * renovarToken() (usado por el job programado), este metodo NO absorbe las
     * excepciones: si la autenticacion falla, propaga el motivo especifico
     * (usuario/password/dispositivo invalido, etc.).
     */
    String obtenerTokenBearer();
}