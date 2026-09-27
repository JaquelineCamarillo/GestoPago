package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoBadResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Traduce las respuestas de error HTTP de GestoPago a excepciones especificas, en vez
 * de dejar pasar un HTTP 400/403 generico. La respuesta de token expirado viene en JSON
 * (segun la documentacion oficial), por eso se busca el campo "message" ahi.
 * Nunca se registra el password ni el token completo en los logs.
 */
@Slf4j
public class GestoPagoFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String body = leerCuerpo(response);
        String mensaje = extraerMensaje(body);

        log.error("GestoPago respondio con error. metodo={}, status={}, mensaje={}", methodKey, status, mensaje);

        if (status == 401 || status == 403) {
            if (esTokenExpirado(body, mensaje)) {
                return new GestoPagoAuthenticationException(GestoPagoAuthenticationException.Motivo.TOKEN_EXPIRADO);
            }
            return new GestoPagoAuthenticationException(inferirMotivoCredenciales(mensaje));
        }

        if (status == 408 || status == 504) {
            return new GestoPagoTimeoutException("GestoPago no respondio a tiempo (timeout)", null);
        }

        if (status >= 500) {
            return new GestoPagoCommunicationException("GestoPago no esta disponible (HTTP " + status + ")", null);
        }

        if (status >= 400) {
            return new GestoPagoBadResponseException(
                    mensaje != null ? mensaje : "GestoPago rechazo la solicitud (HTTP " + status + ")", null);
        }

        return defaultDecoder.decode(methodKey, response);
    }

    private boolean esTokenExpirado(String body, String mensaje) {
        if (body != null && body.toUpperCase(Locale.ROOT).contains("EXPIRED")) {
            return true;
        }
        return mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains("expir");
    }

    private GestoPagoAuthenticationException.Motivo inferirMotivoCredenciales(String mensaje) {
        if (mensaje == null) {
            return GestoPagoAuthenticationException.Motivo.DESCONOCIDO;
        }
        String texto = mensaje.toLowerCase(Locale.ROOT);
        if (texto.contains("password") || texto.contains("contrasen") || texto.contains("contraseñ")) {
            return GestoPagoAuthenticationException.Motivo.PASSWORD_INVALIDO;
        }
        if (texto.contains("dispositivo") || texto.contains("device")) {
            return GestoPagoAuthenticationException.Motivo.DISPOSITIVO_INVALIDO;
        }
        if (texto.contains("distribuidor") || texto.contains("usuario") || texto.contains("user")) {
            return GestoPagoAuthenticationException.Motivo.USUARIO_O_DISTRIBUIDOR_INVALIDO;
        }
        return GestoPagoAuthenticationException.Motivo.DESCONOCIDO;
    }

    private String leerCuerpo(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream is = response.body().asInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("No fue posible leer el cuerpo de la respuesta de error de GestoPago: {}", e.getMessage());
            return null;
        }
    }

    private String extraerMensaje(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("message")) {
                return node.get("message").asText();
            }
            if (node.has("mensaje")) {
                return node.get("mensaje").asText();
            }
        } catch (Exception e) {
            return body.length() > 200 ? body.substring(0, 200) : body;
        }
        return null;
    }
}