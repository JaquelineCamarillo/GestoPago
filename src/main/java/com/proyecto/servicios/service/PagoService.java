package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pagos.ConfirmarPagoRequest;
import com.proyecto.servicios.model.pagos.ConsultarPagoRequest;
import com.proyecto.servicios.model.pagos.IniciarPagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;

public interface PagoService {

    /**
     * Inicia una solicitud de pago o recarga validando sesion, cliente activo, cuenta activa y propia,
     * existencia y restricciones del producto en el catalogo, saldo suficiente y ausencia de duplicados.
     * Genera un codigo de 6 digitos (vigente 5 minutos, maximo 3 intentos) y registra el pago en PENDIENTE_CONFIRMACION.
     */
    PagoResponse iniciarPago(IniciarPagoRequest request);

    /**
     * Confirma un pago verificando el codigo de 6 digitos, retiene el saldo en una transaccion corta
     * con bloqueo pesimista antes de invocar a GestoPago (fuera de transaccion de BD), y aplica el
     * cargo definitivo, reembolso por rechazo o marca EN_REVISION ante timeout/fallo de red.
     */
    PagoResponse confirmarPago(ConfirmarPagoRequest request);

    /**
     * Consulta el estado actual y la bitacora de transiciones de un pago.
     */
    PagoResponse consultarPago(ConsultarPagoRequest request);

    /**
     * Concilia manualmente bajo demanda un pago en estado EN_REVISION invocando confirmTx.do.
     */
    PagoResponse conciliarPago(ConsultarPagoRequest request);

    /**
     * Job programado que concilia los pagos en estado EN_REVISION invocando confirmTx.do.
     */
    void conciliarPagosEnRevision();
}
