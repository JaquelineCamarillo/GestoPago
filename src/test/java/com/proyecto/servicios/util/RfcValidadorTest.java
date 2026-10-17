package com.proyecto.servicios.util;

import com.proyecto.servicios.util.RfcValidador.Tipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RfcValidadorTest {

    // ---------------------------------------------------------- Persona fisica

    @ParameterizedTest(name = "RFC fisica valido: {0}")
    @ValueSource(strings = {
            "CAOJ950518AB9",
            "CAOJ050518K72",
            "HEGG560427XY4",
            "XAXX010101000",   // generico SAT: publico en general
            "XEXX010101000"    // generico SAT: extranjeros
    })
    void rfcFisicaValido_noDevuelveErrores(String rfc) {
        assertTrue(RfcValidador.validar(rfc).isEmpty(), "Deberia ser valido: " + rfc);
        assertTrue(RfcValidador.esValido(rfc));
    }

    @Test
    void normaliza_espaciosExtremosYMinusculas() {
        assertTrue(RfcValidador.esValido("  caoj950518ab9  "));
        assertEquals("CAOJ950518AB9", RfcValidador.normalizar("  caoj950518ab9 "));
    }

    @ParameterizedTest(name = "{0} -> mensaje contiene ''{1}''")
    @CsvSource(delimiter = '|', value = {
            "CAOJ950518AB8|digito verificador",
            "CAOJ951318AB9|fecha de nacimiento",
            "CAOJ950230AB9|fecha de nacimiento",
            "CAOJ950518AB|persona moral",
            "CAOJ950518AB999|13 caracteres",
            "CA0J950518AB9|posiciones 1 a 4",
            "CAOJ95051AAB9|AAMMDD",
            "CAOJ950518A-9|letras y numeros",
            "CAOJ 950518AB9|espacios entre"
    })
    void rfcFisicaInvalido_devuelveMensajeEspecifico(String rfc, String fragmentoEsperado) {
        Optional<String> error = RfcValidador.validar(rfc);
        assertTrue(error.isPresent(), "Deberia ser invalido: " + rfc);
        assertTrue(error.get().contains(fragmentoEsperado),
                "El mensaje '" + error.get() + "' deberia contener '" + fragmentoEsperado + "'");
        assertFalse(RfcValidador.esValido(rfc));
    }

    @ParameterizedTest(name = "vacio, nulo o solo espacios: [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    void rfcVacioONulo_noSeAcepta(String rfc) {
        Optional<String> error = RfcValidador.validar(rfc);
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("no puede estar vacio"));
    }

    @Test
    void extraeFechaDeNacimiento() {
        assertEquals(Optional.of(LocalDate.of(1995, 5, 18)), RfcValidador.fechaNacimiento("CAOJ950518AB9"));
        assertEquals(Optional.of(LocalDate.of(2005, 5, 18)), RfcValidador.fechaNacimiento("CAOJ050518K72"));
    }

    // ------------------------------------------------------------ Persona moral

    @ParameterizedTest(name = "RFC moral valido: {0}")
    @ValueSource(strings = {"SAT970701NN3", "ABC950518XY8"})
    void rfcMoralValido_noDevuelveErrores(String rfc) {
        assertTrue(RfcValidador.validar(rfc, Tipo.MORAL).isEmpty(), "Deberia ser valido: " + rfc);
        assertTrue(RfcValidador.esPersonaMoral(rfc));
    }

    @Test
    void rfcFisicaEnModoMoral_seRechazaConMensajeClaro() {
        Optional<String> error = RfcValidador.validar("CAOJ950518AB9", Tipo.MORAL);
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("persona fisica"));
    }

    @Test
    void rfcGenericoEnModoMoral_seRechaza() {
        Optional<String> error = RfcValidador.validar("XAXX010101000", Tipo.MORAL);
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("generico"));
    }

    // --------------------------------------------------------------- Cualquiera

    @ParameterizedTest(name = "CUALQUIERA acepta: {0}")
    @ValueSource(strings = {"SAT970701NN3", "CAOJ950518AB9", "XAXX010101000"})
    void cualquiera_aceptaFisicaYMoral(String rfc) {
        assertTrue(RfcValidador.esValido(rfc, Tipo.CUALQUIERA));
    }

    @Test
    void cualquiera_rechazaLongitudesIntermedias() {
        Optional<String> error = RfcValidador.validar("CAOJ950518AB99", Tipo.CUALQUIERA);
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("12 caracteres (persona moral) o 13"));
    }
}