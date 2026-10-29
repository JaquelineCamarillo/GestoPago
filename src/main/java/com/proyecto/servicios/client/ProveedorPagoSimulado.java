package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago.EstadoResultado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Implementacion simulada de ProveedorPagoClient (activa por defecto con
 * gestopago.pagos.proveedor=simulado) que emula fielmente los escenarios oficiales
 * del Sandbox de GestoPago/PuntoRed documentados en Postman:
 * - Telefono "9999999991" o referencia que inicia con "TIMEOUT" -> CODIGO=82 (Timeout -> EN_REVISION).
 * - Telefono "6569153326" o referencia que inicia con "RECHAZO" / "000000000" -> CODIGO=02 (Rechazo definitivo -> FALLIDO).
 * - Cualquier otro destino valido -> CODIGO=01 (Operacion realizada con exito -> COMPLETADO).
 * En confirmTx.do:
 * - Si la referencia/telefono termina en "99" -> CODIGO=70 (La transaccion no fue aplicada -> FALLIDO y reembolso).
 * - En caso contrario -> CODIGO=06 (La transaccion fue aplicada exitosamente -> COMPLETADO).
 */
@Component
@ConditionalOnProperty(name = "gestopago.pagos.proveedor", havingValue = "simulado", matchIfMissing = true)
@Slf4j
public class ProveedorPagoSimulado implements ProveedorPagoClient {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public ResultadoProveedorPago enviarTransaccion(GestoPagoSendTxPayload payload) {
        String telefono = payload.getTelefono() != null ? payload.getTelefono().trim() : "";
        String referencia = payload.getReferencia() != null ? payload.getReferencia().trim().toUpperCase() : "";

        log.info("[SIMULADOR GESTOPAGO] sendTx.do para upc={}, idServicio={}, idProducto={}",
                payload.getUpc(), payload.getIdServicio(), payload.getIdProducto());

        if ("9999999991".equals(telefono) || referencia.startsWith("TIMEOUT")) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor("82")
                    .mensajeProveedor("Tiempo de espera alcanzado, la transaccion no ha sido registrada")
                    .numAutorizacion("-1")
                    .build();
        }

        if ("6569153326".equals(telefono)
                || "000000000".equals(referencia)
                || referencia.startsWith("RECHAZO")
                || referencia.startsWith("FAIL")) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.RECHAZO_DEFINITIVO)
                    .codigoProveedor("02")
                    .mensajeProveedor("Transaccion rechazada por el proveedor (referencia invalida o sin saldo de distribuidor)")
                    .numAutorizacion("-1")
                    .build();
        }

        String idTx = String.valueOf(290000000 + RANDOM.nextInt(9999999));
        String numAut = String.valueOf(100000000 + RANDOM.nextInt(899999));

        return ResultadoProveedorPago.builder()
                .estado(EstadoResultado.EXITO)
                .codigoProveedor("01")
                .mensajeProveedor("Operacion realizada con exito")
                .idTx(idTx)
                .numAutorizacion(numAut)
                .build();
    }

    @Override
    public ResultadoProveedorPago confirmarTransaccion(GestoPagoSendTxPayload payload) {
        String telefono = payload.getTelefono() != null ? payload.getTelefono().trim() : "";
        String referencia = payload.getReferencia() != null ? payload.getReferencia().trim().toUpperCase() : "";

        log.info("[SIMULADOR GESTOPAGO] confirmTx.do para upc={}", payload.getUpc());

        if (telefono.endsWith("99") || referencia.endsWith("NOAP")) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.RECHAZO_DEFINITIVO)
                    .codigoProveedor("70")
                    .mensajeProveedor("La transaccion no fue aplicada")
                    .idTx("-1")
                    .numAutorizacion("0")
                    .build();
        }

        String idTx = String.valueOf(290000000 + RANDOM.nextInt(9999999));
        String numAut = String.valueOf(100000000 + RANDOM.nextInt(899999));

        return ResultadoProveedorPago.builder()
                .estado(EstadoResultado.EXITO)
                .codigoProveedor("06")
                .mensajeProveedor("La transaccion fue aplicada exitosamente. Autorizacion [" + numAut + "] idTicket[" + idTx + "]")
                .idTx(idTx)
                .numAutorizacion(numAut)
                .build();
    }
}
