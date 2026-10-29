package com.proyecto.servicios.client;

import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxMensajeXml;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago.EstadoResultado;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.util.GestoPagoAesCipher;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.util.function.Function;

/**
 * Implementacion real de ProveedorPagoClient que consume GestoPago (sendTx.do y confirmTx.do).
 * Se activa cuando gestopago.pagos.proveedor=real.
 * Cifra el payload JSON con AES-256-CBC y envia el token Bearer en la cabecera Authorization.
 * Jamas registra en logs el token ni el payload cifrado completo.
 */
@Component
@ConditionalOnProperty(name = "gestopago.pagos.proveedor", havingValue = "real")
@Slf4j
public class ProveedorPagoGestoPagoReal implements ProveedorPagoClient {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CODIGO_EXITO_SEND_TX = "01";
    private static final String CODIGO_TIMEOUT_SEND_TX = "82";
    private static final String CODIGO_APLICADA_CONFIRM_TX = "06";
    private static final String CODIGO_NO_APLICADA_CONFIRM_TX = "70";

    private final GestoPagoTxClient gestoPagoTxClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoAesCipher aesCipher;

    public ProveedorPagoGestoPagoReal(GestoPagoTxClient gestoPagoTxClient,
                                      GestoPagoTokenService gestoPagoTokenService,
                                      GestoPagoAesCipher aesCipher) {
        this.gestoPagoTxClient = gestoPagoTxClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.aesCipher = aesCipher;
    }

    @Override
    public ResultadoProveedorPago enviarTransaccion(GestoPagoSendTxPayload payload) {
        String signed = aesCipher.cifrarPayload(payload);
        String random = String.valueOf(System.currentTimeMillis());

        log.info("Invocando sendTx.do en GestoPago para upc={}, idServicio={}, idProducto={}",
                payload.getUpc(), payload.getIdServicio(), payload.getIdProducto());

        try {
            GestoPagoSendTxXmlResponse xml = invocarConReintentoToken(
                    auth -> gestoPagoTxClient.sendTx(auth, random, signed));
            return interpretarRespuestaSendTx(xml);
        } catch (GestoPagoTimeoutException | RetryableException e) {
            log.warn("Timeout al invocar sendTx.do para upc={}: se marcara EN_REVISION para conciliar con confirmTx.do",
                    payload.getUpc());
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor(CODIGO_TIMEOUT_SEND_TX)
                    .mensajeProveedor("GestoPago no respondio a tiempo (timeout); pago en revision para conciliacion")
                    .build();
        } catch (GestoPagoCommunicationException e) {
            log.warn("Error de comunicacion al invocar sendTx.do para upc={}: se marcara EN_REVISION",
                    payload.getUpc());
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor("HTTP_5XX")
                    .mensajeProveedor("Error de comunicacion con GestoPago; pago en revision para conciliacion")
                    .build();
        } catch (GestoPagoAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            if (tieneCausaTimeout(e)) {
                return ResultadoProveedorPago.builder()
                        .estado(EstadoResultado.REQUIERE_CONCILIACION)
                        .codigoProveedor(CODIGO_TIMEOUT_SEND_TX)
                        .mensajeProveedor("Tiempo de espera agotado con GestoPago; pago en revision")
                        .build();
            }
            log.error("Error inesperado durante sendTx.do para upc={}: {}", payload.getUpc(), e.getMessage());
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor("ERROR_RED")
                    .mensajeProveedor("Respuesta ambigua o fallo de red con el proveedor; pago en revision")
                    .build();
        }
    }

    @Override
    public ResultadoProveedorPago confirmarTransaccion(GestoPagoSendTxPayload payload) {
        String signed = aesCipher.cifrarPayload(payload);
        String random = String.valueOf(System.currentTimeMillis());

        log.info("Invocando confirmTx.do en GestoPago para conciliar upc={}", payload.getUpc());

        try {
            GestoPagoSendTxXmlResponse xml = invocarConReintentoToken(
                    auth -> gestoPagoTxClient.confirmTx(auth, random, signed));
            return interpretarRespuestaConfirmTx(xml);
        } catch (Exception e) {
            log.warn("No fue posible conciliar upc={} en este intento: {}", payload.getUpc(), e.getMessage());
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor("PENDIENTE")
                    .mensajeProveedor("El proveedor aun no confirma el estado de la transaccion")
                    .build();
        }
    }

    private GestoPagoSendTxXmlResponse invocarConReintentoToken(
            Function<String, GestoPagoSendTxXmlResponse> llamada) {
        String token = gestoPagoTokenService.obtenerTokenBearer();
        try {
            return llamada.apply(BEARER_PREFIX + token);
        } catch (GestoPagoAuthenticationException e) {
            if (e.getMotivo() == GestoPagoAuthenticationException.Motivo.TOKEN_EXPIRADO) {
                log.warn("Token GestoPago expirado al procesar pago; renovando token y reintentando una vez");
                gestoPagoTokenService.renovarToken();
                String nuevoToken = gestoPagoTokenService.obtenerTokenBearer();
                return llamada.apply(BEARER_PREFIX + nuevoToken);
            }
            throw e;
        }
    }

    private ResultadoProveedorPago interpretarRespuestaSendTx(GestoPagoSendTxXmlResponse xml) {
        if (xml == null || xml.getMensaje() == null || xml.getMensaje().getCodigo() == null) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor("SIN_CODIGO")
                    .mensajeProveedor("Respuesta vacia o incompleta de GestoPago en sendTx.do")
                    .build();
        }

        GestoPagoSendTxMensajeXml msg = xml.getMensaje();
        String codigo = msg.getCodigo().trim();
        String texto = msg.getTexto() != null ? msg.getTexto().trim() : "Sin mensaje del proveedor";

        if (CODIGO_EXITO_SEND_TX.equals(codigo)) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.EXITO)
                    .codigoProveedor(codigo)
                    .mensajeProveedor(texto)
                    .idTx(xml.getIdTx())
                    .numAutorizacion(xml.getNumAutorizacion())
                    .pin(msg.getPin())
                    .legend(msg.getLegend())
                    .build();
        }

        if (CODIGO_TIMEOUT_SEND_TX.equals(codigo)) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor(codigo)
                    .mensajeProveedor(texto)
                    .build();
        }

        return ResultadoProveedorPago.builder()
                .estado(EstadoResultado.RECHAZO_DEFINITIVO)
                .codigoProveedor(codigo)
                .mensajeProveedor(texto)
                .idTx(xml.getIdTx())
                .numAutorizacion(xml.getNumAutorizacion())
                .build();
    }

    private ResultadoProveedorPago interpretarRespuestaConfirmTx(GestoPagoSendTxXmlResponse xml) {
        if (xml == null || xml.getMensaje() == null || xml.getMensaje().getCodigo() == null) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.REQUIERE_CONCILIACION)
                    .codigoProveedor("SIN_CODIGO")
                    .mensajeProveedor("Respuesta vacia de GestoPago en confirmTx.do")
                    .build();
        }

        GestoPagoSendTxMensajeXml msg = xml.getMensaje();
        String codigo = msg.getCodigo().trim();
        String texto = msg.getTexto() != null ? msg.getTexto().trim() : "Sin mensaje del proveedor";
        String idTx = msg.getIdTx() != null ? msg.getIdTx() : xml.getIdTx();

        if (CODIGO_APLICADA_CONFIRM_TX.equals(codigo) || CODIGO_EXITO_SEND_TX.equals(codigo)) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.EXITO)
                    .codigoProveedor(codigo)
                    .mensajeProveedor(texto)
                    .idTx(idTx)
                    .numAutorizacion(xml.getNumAutorizacion())
                    .build();
        }

        if (CODIGO_NO_APLICADA_CONFIRM_TX.equals(codigo)) {
            return ResultadoProveedorPago.builder()
                    .estado(EstadoResultado.RECHAZO_DEFINITIVO)
                    .codigoProveedor(codigo)
                    .mensajeProveedor(texto)
                    .build();
        }

        return ResultadoProveedorPago.builder()
                .estado(EstadoResultado.REQUIERE_CONCILIACION)
                .codigoProveedor(codigo)
                .mensajeProveedor(texto)
                .build();
    }

    private boolean tieneCausaTimeout(Throwable t) {
        Throwable actual = t;
        while (actual != null) {
            if (actual instanceof SocketTimeoutException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
