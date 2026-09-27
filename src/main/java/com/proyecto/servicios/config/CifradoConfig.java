package com.proyecto.servicios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/** Cifrador AES compartido para password, token y datos faciales del login. */
@Configuration
public class CifradoConfig {

    @Value("${app.encryption.password}")
    private String password;

    @Value("${app.encryption.salt}")
    private String salt;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(password, salt);
    }
}