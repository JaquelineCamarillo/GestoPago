package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import java.util.UUID;

/** Cifrador AES compartido para password, token y datos faciales del login. */
@Slf4j
@Configuration
public class CifradoConfig {

    @Value("${app.encryption.password:}")
    private String password;

    @Value("${app.encryption.salt:}")
    private String salt;

    @Bean
    public TextEncryptor textEncryptor() {
        String claveEfectiva = (password != null && !password.isBlank())
                ? password
                : UUID.randomUUID().toString();
        String saltEfectivo = (salt != null && !salt.isBlank())
                ? salt
                : KeyGenerators.string().generateKey();
        if (password == null || password.isBlank() || salt == null || salt.isBlank()) {
            log.warn("Variables APP_ENCRYPTION_PASSWORD o APP_ENCRYPTION_SALT no definidas; usando llave efimera generada en memoria.");
        }
        return Encryptors.text(claveEfectiva, saltEfectivo);
    }
}
