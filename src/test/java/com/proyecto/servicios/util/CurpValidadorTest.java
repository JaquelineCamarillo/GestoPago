package com.proyecto.servicios.util;

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

class CurpValidadorTest {

    @ParameterizedTest(name = "CURP valida: {0}")
    @ValueSource(strings = {
            "HEGG560427MVZRRL04",   // nacio antes del 2000 (homoclave numero)
            "CAOJ950518MGTMLQ08",
            "CAOJ050518MGTMLQA4",   // nacio en 2005 (homoclave letra)
            "CAOJ950518XGTMLQ02"    // sexo X (no binario)
    })
    void curpValida_noDevuelveErrores(String curp) {
        assertTrue(CurpValidador.validar(curp).isEmpty(), "Deberia ser valida: " + curp);
        assertTrue(CurpValidador.esValida(curp));
    }

    @Test
    void normaliza_espaciosYMinusculas() {
        assertTrue(CurpValidador.esValida("  caoj950518mgtmlq08  "));
        assertEquals("CAOJ950518MGTMLQ08", CurpValidador.normalizar("  caoj950518mgtmlq08 "));
    }

    @ParameterizedTest(name = "{0} -> mensaje contiene ''{1}''")
    @CsvSource(delimiter = '|', value = {
            "CAOJ950518MGTMLQ09|digito verificador",
            "CAOJ951318MGTMLQ08|fecha de nacimiento",
            "CAOJ950230MGTMLQ08|fecha de nacimiento",
            "CAOJ950518MZZMLQ08|entidad federativa",
            "CAOJ950518MGTMLQ0|18 caracteres",
            "CAOJ950518MGTMLQ0-|letras y numeros",
            "CAOJ950518ZGTMLQ08|sexo",
            "CA0J950518MGTMLQ08|posiciones 1 a 4",
            "CAOJ950518MGTAEQ08|consonantes"
    })
    void curpInvalida_devuelveMensajeEspecifico(String curp, String fragmentoEsperado) {
        Optional<String> error = CurpValidador.validar(curp);
        assertTrue(error.isPresent(), "Deberia ser invalida: " + curp);
        assertTrue(error.get().contains(fragmentoEsperado),
                "El mensaje '" + error.get() + "' deberia contener '" + fragmentoEsperado + "'");
        assertFalse(CurpValidador.esValida(curp));
    }

    @ParameterizedTest(name = "vacia o nula: [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void curpVaciaONula_esObligatoria(String curp) {
        Optional<String> error = CurpValidador.validar(curp);
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("obligatoria"));
    }

    @Test
    void extraeFechaDeNacimiento_antesYDespuesDel2000() {
        assertEquals(LocalDate.of(1995, 5, 18), CurpValidador.fechaNacimiento("CAOJ950518MGTMLQ08"));
        assertEquals(LocalDate.of(2005, 5, 18), CurpValidador.fechaNacimiento("CAOJ050518MGTMLQA4"));
    }

    @Test
    void extraeSexo() {
        assertEquals('M', CurpValidador.sexo("CAOJ950518MGTMLQ08"));
        assertEquals('X', CurpValidador.sexo("CAOJ950518XGTMLQ02"));
    }
}