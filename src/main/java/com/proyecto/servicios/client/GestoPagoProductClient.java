package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoFeignConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente de integracion (capa Client/Integration) para el catalogo de productos de
 * GestoPago: GET /sistema/service/getProductList.do
 * La respuesta viene en XML, no JSON.
 */
@FeignClient(name = "gestoPagoProduct", url = "${gestopago.api.url}", configuration = GestoPagoFeignConfig.class)
public interface GestoPagoProductClient {

    @GetMapping(value = "/sistema/service/getProductList.do", produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoProductListXmlResponse obtenerListaProductos(@RequestHeader("Authorization") String authorizationHeader);
}