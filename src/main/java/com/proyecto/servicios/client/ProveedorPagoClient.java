package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago;

/**
 * Abstraccion del proveedor de pagos (principio de inversion de dependencias - SOLID).
 * Permite conmutar entre el proveedor real (GestoPago sendTx.do / confirmTx.do) y el
 * proveedor simulado mediante la propiedad gestopago.pagos.proveedor.
 */
public interface ProveedorPagoClient {

    /**
     * Ejecuta el cobro de una recarga o pago de servicio ante el proveedor (sendTx.do).
     */
    ResultadoProveedorPago enviarTransaccion(GestoPagoSendTxPayload payload);

    /**
     * Consulta/concilia el estado real de una transaccion previa que quedo en duda (confirmTx.do).
     * Debe enviarse exactamente el mismo payload utilizado en sendTx.do.
     */
    ResultadoProveedorPago confirmarTransaccion(GestoPagoSendTxPayload payload);
}
