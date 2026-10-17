package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    /**
     * La URL del servidor es relativa ("/"): Swagger llama al mismo host desde el que se abrio,
     * asi funciona igual en local (localhost:8081) y en Render sin cambiar codigo.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Integracion de Clientes - GestoPago")
                        .version("1.0")
                        .description("API de onboarding de clientes personas fisicas y catalogo de productos GestoPago"))
                .addServersItem(new Server().url("/").description("Servidor actual"));
    }
}