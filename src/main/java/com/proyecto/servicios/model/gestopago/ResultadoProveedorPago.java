package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Resultado interpretado de una llamada al proveedor de pagos (sendTx.do o confirmTx.do).
 */
@Getter
@Builder
@AllArgsConstructor
public class ResultadoProveedorPago {

    public enum EstadoResultado {
        /** CODIGO=01 en sendTx o CODIGO=06 en confirmTx: el pago fue aplicado con exito. */
        EXITO,
        /** Rechazo claro y definitivo del proveedor (ej. CODIGO=02, 06 en sendTx, 70 en confirmTx, 100, 101). */
        RECHAZO_DEFINITIVO,
        /** Timeout (CODIGO=82 o timeout de red) o respuesta ambigua/caida: requiere conciliacion (EN_REVISION). */
        REQUIERE_CONCILIACION
    }

    private final EstadoResultado estado;
    private final String codigoProveedor;
    private final String mensajeProveedor;
    private final String idTx;
    private final String numAutorizacion;
    private final String pin;
    private final String legend;
}
