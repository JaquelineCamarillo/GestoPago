package com.proyecto.servicios.enums;

/**
 * Bandera del tipo de operacion solicitada por el cliente.
 * RECARGA corresponde a productos de tiempo aire o pines (obligatorio telefono de 10 digitos).
 * SERVICIO corresponde a pago de recibos/facturas (obligatoria referencia alfanumerica).
 */
public enum TipoPago {
    RECARGA,
    SERVICIO
}
