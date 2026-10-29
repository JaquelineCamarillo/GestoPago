package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoFeignConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign para ejecutar y conciliar transacciones en GestoPago:
 * - POST /sistema/service/sendTx.do
 * - POST /sistema/service/confirmTx.do
 * Segun la documentacion oficial, los parametros "random" y "signed" (payload JSON cifrado en AES-256-CBC)
 * se envian en la peticion POST autenticada con Authorization: Bearer <token>, y la respuesta es XML.
 */
@FeignClient(name = "gestoPagoTx", url = "${gestopago.api.url}", configuration = GestoPagoFeignConfig.class)
public interface GestoPagoTxClient {

    @PostMapping(
            value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    GestoPagoSendTxXmlResponse sendTx(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );

    @PostMapping(
            value = "/sistema/service/confirmTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    GestoPagoSendTxXmlResponse confirmTx(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );
}
