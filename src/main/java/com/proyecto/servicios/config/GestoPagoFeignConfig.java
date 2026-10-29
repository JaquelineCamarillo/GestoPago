package com.proyecto.servicios.config;

import com.proyecto.servicios.client.GestoPagoFeignErrorDecoder;
import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuracion comun para los clientes Feign de GestoPago (autenticacion, catalogo y pagos).
 * NO lleva @Configuration a nivel de clase a proposito: se usa como
 * "configuration = GestoPagoFeignConfig.class" dentro de cada @FeignClient, para que
 * Spring NO la registre como bean global y no se aplique a otros clientes del sistema.
 * El nivel de log de Feign se fija en NONE para jamas imprimir el header Authorization
 * ni los cuerpos cifrados o credenciales.
 */
public class GestoPagoFeignConfig {

    @Value("${gestopago.client.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${gestopago.client.read-timeout-ms:15000}")
    private long readTimeoutMs;

    @Bean
    public ErrorDecoder errorDecoder() {
        return new GestoPagoFeignErrorDecoder();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.NONE;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(
                connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS,
                true);
    }
}