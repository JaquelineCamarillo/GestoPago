package com.proyecto.servicios.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoAesCipherTest {

    private static final String TEST_SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String TEST_IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    @Test
    void cifrarYDescifrarTexto_conLlavesDeDocumentacionGestoPago_esReversible() {
        GestoPagoAesCipher cipher = new GestoPagoAesCipher(new ObjectMapper(), TEST_SECRET_KEY, TEST_IV);
        String jsonEjemplo = "{\"idServicio\":79, \"idProducto\":209 , \"referencia\": \"40425475190118187271\", \"montoPago\": 9999, \"telefono\":\"1111111111\", \"horaLocal\":\"20200401222821\"}";

        String cifrado = cipher.cifrarTexto(jsonEjemplo, TEST_SECRET_KEY, TEST_IV);
        assertNotNull(cifrado);
        assertEquals(
                "DB8BRqb2q6BW/HLQYN2pr2n9DTL1Q8kp2lvZi3rogbxjEasiMbgU4q5/vDav+p0O0KWlfMmNekXN+UkbUiB+s/LNf1MF2EgOQEZoivxgp+UJxsuT5vDIMmQXUkuwkyUE+a7hH5FwaDDY8DNwW2kowFXeE69AcOMaWnyZ+YplKNEUOzQzLstBxWnJE+aSr0+vQN3knkIIjbT10yfSTV/OQA==",
                cifrado
        );

        String descifrado = cipher.descifrarTexto(cifrado, TEST_SECRET_KEY, TEST_IV);
        assertEquals(jsonEjemplo, descifrado);
    }

    @Test
    void cifrarPayload_serializaCorrectamenteYCifra() {
        ObjectMapper mapper = new ObjectMapper();
        GestoPagoAesCipher cipher = new GestoPagoAesCipher(mapper, TEST_SECRET_KEY, TEST_IV);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(205)
                .idServicio(76)
                .telefono("1111111111")
                .horaLocal("20220101235900")
                .referencia("25720381")
                .montoPago(new BigDecimal("601.99"))
                .upc("AB12CD34EF56")
                .unidad("083")
                .build();

        String cifrado = cipher.cifrarPayload(payload);
        assertNotNull(cifrado);
        String jsonDescifrado = cipher.descifrarTexto(cifrado, TEST_SECRET_KEY, TEST_IV);
        assertTrue(jsonDescifrado.contains("\"upc\":\"AB12CD34EF56\""));
        assertTrue(jsonDescifrado.contains("\"referencia\":\"25720381\""));
    }

    @Test
    void cifrarTexto_sinLlavesConfiguradas_lanzaExcepcionControlada() {
        GestoPagoAesCipher cipher = new GestoPagoAesCipher(new ObjectMapper(), "", "");
        assertThrows(GestoPagoCommunicationException.class,
                () -> cipher.cifrarPayload(GestoPagoSendTxPayload.builder().idProducto(1).idServicio(1).build()));
    }
}
