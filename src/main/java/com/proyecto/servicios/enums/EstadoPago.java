package com.proyecto.servicios.enums;

/**
 * Estados del ciclo de vida de un pago bajo la estrategia retener -> cobrar -> conciliar.
 */
public enum EstadoPago {
    /** Solicitud registrada; codigo de 6 digitos enviado, sin retener saldo aun. */
    PENDIENTE_CONFIRMACION,
    /** Codigo verificado y monto retenido en base de datos; invocando a GestoPago. */
    EN_PROCESO,
    /** Proveedor confirmo exito (CODIGO=01 o confirmTx=06); retencion convertida en cargo definitivo. */
    COMPLETADO,
    /** Solicitud rechazada por validacion o por agotamiento de intentos de confirmacion. */
    RECHAZADO,
    /** El codigo de confirmacion de 6 digitos vencio (mas de 5 minutos) sin confirmarse. */
    EXPIRADO,
    /** El proveedor rechazo de forma definitiva la transaccion; la retencion fue liberada al saldo. */
    FALLIDO,
    /** Timeout o error de red con el proveedor; la retencion se mantiene hasta conciliar con confirmTx.do. */
    EN_REVISION
}
