package com.proyecto.servicios.client;

import com.proyecto.servicios.model.RestCountriesListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente de la API publica REST Countries (v5).
 * La clave se envia en el encabezado Authorization: Bearer {clave}; viene de una variable de entorno.
 */
@FeignClient(name = "restCountriesClient", url = "${restcountries.base-url:https://api.restcountries.com}")
public interface RestCountriesClient {

    /**
     * Lista paises de forma paginada (maximo 100 por pagina en el plan gratis).
     *
     * @param responseFields campos a devolver, separados por coma (ej. "names.common,names.translations,codes.alpha_2")
     */
    @GetMapping("/countries/v5")
    RestCountriesListResponse listarPaises(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("limit") int limit,
            @RequestParam("offset") int offset,
            @RequestParam("response_fields") String responseFields);
}