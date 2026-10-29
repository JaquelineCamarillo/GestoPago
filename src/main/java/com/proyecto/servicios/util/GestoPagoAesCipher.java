package com.proyecto.servicios.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cifra y descifra el payload JSON de transacciones GestoPago (sendTx.do / confirmTx.do)
 * utilizando el algoritmo oficial documentado por PuntoRed/GestoPago:
 * AES/CBC/PKCS5Padding con SECRET_KEY (32 bytes en Base64) e IV (16 bytes en Base64).
 * Jamas imprime llaves ni datos sensibles en logs.
 */
@Component
public class GestoPagoAesCipher {

    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";
    private static final String ESPECIFICACION_LLAVE = "AES";

    private final ObjectMapper objectMapper;
    private final String secretKeyBase64;
    private final String ivBase64;

    public GestoPagoAesCipher(
            ObjectMapper objectMapper,
            @Value("${gestopago.pagos.secret-key:}") String secretKeyBase64,
            @Value("${gestopago.pagos.iv:}") String ivBase64) {
        this.objectMapper = objectMapper;
        this.secretKeyBase64 = secretKeyBase64;
        this.ivBase64 = ivBase64;
    }

    /** Serializa el payload a JSON y lo cifra en Base64 con la llave e IV configurados. */
    public String cifrarPayload(GestoPagoSendTxPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return cifrarTexto(json, secretKeyBase64, ivBase64);
        } catch (GestoPagoCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new GestoPagoCommunicationException("No fue posible serializar y cifrar la peticion hacia GestoPago", e);
        }
    }

    /** Cifra un texto plano UTF-8 usando AES/CBC/PKCS5Padding con llave e IV en Base64. */
    public String cifrarTexto(String textoPlano, String keyBase64, String vectorInicializacionBase64) {
        validarCredencialesCifrado(keyBase64, vectorInicializacionBase64);
        try {
            byte[] ivBytes = Base64.getDecoder().decode(vectorInicializacionBase64.trim());
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ESPECIFICACION_LLAVE);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cifrado);
        } catch (Exception e) {
            throw new GestoPagoCommunicationException("Error al aplicar el cifrado AES-256-CBC de GestoPago", e);
        }
    }

    /** Descifra una cadena en Base64 usando AES/CBC/PKCS5Padding con llave e IV en Base64. */
    public String descifrarTexto(String textoCifradoBase64, String keyBase64, String vectorInicializacionBase64) {
        validarCredencialesCifrado(keyBase64, vectorInicializacionBase64);
        try {
            byte[] ivBytes = Base64.getDecoder().decode(vectorInicializacionBase64.trim());
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ESPECIFICACION_LLAVE);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] descifrado = cipher.doFinal(Base64.getDecoder().decode(textoCifradoBase64.trim()));
            return new String(descifrado, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new GestoPagoCommunicationException("Error al descifrar el payload AES-256-CBC de GestoPago", e);
        }
    }

    private void validarCredencialesCifrado(String keyBase64, String vectorInicializacionBase64) {
        if (keyBase64 == null || keyBase64.isBlank()
                || vectorInicializacionBase64 == null || vectorInicializacionBase64.isBlank()) {
            throw new GestoPagoCommunicationException(
                    "Faltan las variables de cifrado de GestoPago (GESTOPAGO_SECRET_KEY y GESTOPAGO_IV)", null);
        }
    }
}
