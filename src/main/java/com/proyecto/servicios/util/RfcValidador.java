package com.proyecto.servicios.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validador del RFC de persona fisica (13 caracteres).
 *
 * Estructura:
 *  1-4   : 4 letras (la 2a es vocal o X; se permite la Ñ)
 *  5-10  : fecha de nacimiento AAMMDD (debe ser una fecha real y no futura)
 *  11-12 : homoclave (letras o numeros)
 *  13    : digito verificador (numero del 0 al 9 o la letra A)
 */
public final class RfcValidador {

    /** Poner en false si se quiere aceptar RFC con digito verificador incorrecto. */
    public static final boolean VALIDAR_DIGITO_VERIFICADOR = true;

    // \u00D1 = Ñ (con escape para evitar problemas de codificacion en Windows)
    private static final String DICCIONARIO = "0123456789ABCDEFGHIJKLMN&OPQRSTUVWXYZ \u00D1";

    private static final Pattern SOLO_VALIDOS = Pattern.compile("^[A-Z0-9\u00D1&]+$");
    private static final Pattern LETRAS_INICIALES = Pattern.compile("^[A-Z\u00D1][AEIOUX][A-Z\u00D1]{2}$");
    private static final Pattern HOMOCLAVE = Pattern.compile("^[A-Z0-9]{2}$");

    private RfcValidador() {
    }

    /** Normaliza: quita espacios de los extremos y pasa a mayusculas. */
    public static String normalizar(String rfc) {
        return rfc == null ? null : rfc.trim().toUpperCase();
    }

    /** Devuelve el mensaje del primer error encontrado, o vacio si el RFC es valido. */
    public static Optional<String> validar(String valor) {
        String rfc = normalizar(valor);

        if (rfc == null || rfc.isEmpty()) {
            return Optional.of("El RFC es obligatorio");
        }
        if (rfc.length() == 12) {
            return Optional.of("El RFC tiene 12 caracteres, que corresponde a una persona moral. "
                    + "Este registro es de persona fisica y debe tener 13 caracteres");
        }
        if (rfc.length() != 13) {
            return Optional.of("El RFC de persona fisica debe tener exactamente 13 caracteres y tiene " + rfc.length());
        }
        if (!SOLO_VALIDOS.matcher(rfc).matches()) {
            return Optional.of("El RFC solo puede contener letras y numeros, sin espacios, acentos ni simbolos");
        }

        if (!LETRAS_INICIALES.matcher(rfc.substring(0, 4)).matches()) {
            return Optional.of("Las posiciones 1 a 4 del RFC deben ser letras y la segunda debe ser vocal o X");
        }

        String fecha = rfc.substring(4, 10);
        if (!fecha.chars().allMatch(Character::isDigit)) {
            return Optional.of("Las posiciones 5 a 10 del RFC deben ser la fecha de nacimiento en formato AAMMDD (solo numeros)");
        }
        if (fechaNacimiento(rfc).isEmpty()) {
            return Optional.of("La fecha de nacimiento dentro del RFC (" + fecha + ") no es una fecha valida");
        }

        if (!HOMOCLAVE.matcher(rfc.substring(10, 12)).matches()) {
            return Optional.of("Las posiciones 11 y 12 del RFC (homoclave) deben ser letras o numeros");
        }

        char ultimo = rfc.charAt(12);
        if (!(Character.isDigit(ultimo) || ultimo == 'A')) {
            return Optional.of("La posicion 13 del RFC (digito verificador) debe ser un numero o la letra A");
        }
        if (VALIDAR_DIGITO_VERIFICADOR && ultimo != digitoVerificador(rfc)) {
            return Optional.of("El digito verificador del RFC es incorrecto (deberia ser "
                    + digitoVerificador(rfc) + ")");
        }

        return Optional.empty();
    }

    public static boolean esValido(String rfc) {
        return validar(rfc).isEmpty();
    }

    /**
     * Fecha contenida en el RFC. El RFC no indica el siglo, asi que se toma el mas
     * reciente que no sea futuro. Vacio si la fecha no existe en ningun siglo.
     */
    public static Optional<LocalDate> fechaNacimiento(String valor) {
        String rfc = normalizar(valor);
        int anio = Integer.parseInt(rfc.substring(4, 6));
        int mes = Integer.parseInt(rfc.substring(6, 8));
        int dia = Integer.parseInt(rfc.substring(8, 10));
        LocalDate hoy = LocalDate.now();

        for (int siglo : new int[] {2000, 1900}) {
            try {
                LocalDate fecha = LocalDate.of(siglo + anio, mes, dia);
                if (!fecha.isAfter(hoy)) {
                    return Optional.of(fecha);
                }
            } catch (DateTimeException ignorada) {
                // se prueba con el otro siglo (ej. 29 de febrero)
            }
        }
        return Optional.empty();
    }

    private static char digitoVerificador(String rfc) {
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            suma += DICCIONARIO.indexOf(rfc.charAt(i)) * (13 - i);
        }
        int resultado = 11 - (suma % 11);
        if (resultado == 11) {
            return '0';
        }
        if (resultado == 10) {
            return 'A';
        }
        return (char) ('0' + resultado);
    }
}