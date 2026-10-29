package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Cliente;

/**
 * Canal de entrega del codigo de segunda verificacion (6 digitos) para autorizar un pago.
 * En modo demo se entrega por bitacora controlada y campo codigoDemo;
 * puede extenderse con una implementacion de correo electronico, SMS o verificacion biometrica.
 */
public interface NotificadorCodigo {

    /**
     * Entrega el codigo de verificacion al cliente y devuelve el valor para exposicion en modo demo
     * (o null si la implementacion es de produccion por correo/SMS).
     */
    String enviarCodigoConfirmacion(Cliente cliente, Integer pagoId, String codigo);
}
