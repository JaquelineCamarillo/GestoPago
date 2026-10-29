package com.proyecto.servicios.util;

import com.proyecto.servicios.validation.NumeroCuenta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumeroCuentaValidadorTest {

    @Test
    void generador_produceDieciseisDigitosNumericosSinCeroInicial() {
        for (int i = 0; i < 50; i++) {
            String cuenta = GeneradorNumeroCuenta.generar();
            assertEquals(16, cuenta.length());
            assertTrue(NumeroCuenta.Validador.esValido(cuenta));
            assertNotEquals('0', cuenta.charAt(0));
        }
    }

    @Test
    void validador_rechazaLongitudesDiferentesLetrasYEspacios() {
        assertTrue(NumeroCuenta.Validador.esValido("1234567890123456"));
        assertFalse(NumeroCuenta.Validador.esValido(null));
        assertFalse(NumeroCuenta.Validador.esValido(""));
        assertFalse(NumeroCuenta.Validador.esValido("123456789012345"));
        assertFalse(NumeroCuenta.Validador.esValido("12345678901234567"));
        assertFalse(NumeroCuenta.Validador.esValido("123456789012345A"));
        assertFalse(NumeroCuenta.Validador.esValido("1234 56789012345"));
        assertFalse(NumeroCuenta.Validador.esValido("176123456789A1B2"));
    }
}
