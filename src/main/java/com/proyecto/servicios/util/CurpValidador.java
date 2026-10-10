package com.proyecto.servicios.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador de la CURP (18 caracteres).
 *
 * Estructura:
 *  1-4   : 4 letras (la 2a es vocal o X)
 *  5-10  : fecha de nacimiento AAMMDD (debe ser una fecha real y no futura)
 *  11    : sexo H, M o X (no binario)
 *  12-13 : clave de entidad federativa (catalogo oficial)
 *  14-16 : 3 consonantes internas
 *  17    : homoclave (digito si nacio antes del 2000, letra si nacio del 2000 en adelante)
 *  18    : digito verificador (se calcula con las 17 posiciones anteriores)
 */
public final class CurpValidador {

    /** Poner en false si se quiere aceptar CURP inventadas con digito verificador incorrecto. */
    public static final boolean VALIDAR_DIGITO_VERIFICADOR = true;

    // \u00D1 = Ñ (se escribe con escape para evitar problemas de codificacion en Windows)
    private static final String DICCIONARIO = "0123456789ABCDEFGHIJKLMN\u00D1OPQRSTUVWXYZ";

    private static final Set<String> ENTIDADES = Set.of(
            "AS", "BC", "BS", "CC", "CL", "CM", "CS", "CH", "DF", "DG", "GT", "GR",
            "HG", "JC", "MC", "MN", "MS", "NT", "NL", "OC", "PL", "QT", "QR", "SP",
            "SL", "SR", "TC", "TS", "TL", "VZ", "YN", "ZS", "NE");

    private static final Pattern SOLO_ALFANUMERICO = Pattern.compile("^[A-Z0-9]+$");
    private static final Pattern LETRAS_INICIALES = Pattern.compile("^[A-Z][AEIOUX][A-Z]{2}$");
    private static final Pattern CONSONANTES = Pattern.compile("^[B-DF-HJ-NP-TV-Z]{3}$");

    private CurpValidador() {
    }

    /** Normaliza: quita espacios de los extremos y pasa a mayusculas. */
    public static String normalizar(String curp) {
        return curp == null ? null : curp.trim().toUpperCase();
    }

    /** Devuelve el mensaje del primer error encontrado, o vacio si la CURP es valida. */
    public static Optional<String> validar(String valor) {
        String curp = normalizar(valor);

        if (curp == null || curp.isEmpty()) {
            return Optional.of("La CURP es obligatoria");
        }
        if (curp.length() != 18) {
            return Optional.of("La CURP debe tener exactamente 18 caracteres y tiene " + curp.length());
        }
        if (!SOLO_ALFANUMERICO.matcher(curp).matches()) {
            return Optional.of("La CURP solo puede contener letras y numeros, sin espacios, acentos ni simbolos");
        }

        String letras = curp.substring(0, 4);
        if (!LETRAS_INICIALES.matcher(letras).matches()) {
            return Optional.of("Las posiciones 1 a 4 de la CURP deben ser letras y la segunda debe ser vocal o X");
        }

        String fecha = curp.substring(4, 10);
        if (!fecha.chars().allMatch(Character::isDigit)) {
            return Optional.of("Las posiciones 5 a 10 de la CURP deben ser la fecha de nacimiento en formato AAMMDD (solo numeros)");
        }
        LocalDate nacimiento;
        try {
            nacimiento = fechaNacimiento(curp);
        } catch (DateTimeException e) {
            return Optional.of("La fecha de nacimiento dentro de la CURP (" + fecha + ") no es una fecha valida");
        }
        if (nacimiento.isAfter(LocalDate.now())) {
            return Optional.of("La fecha de nacimiento dentro de la CURP no puede ser futura");
        }

        char sexo = curp.charAt(10);
        if (sexo != 'H' && sexo != 'M' && sexo != 'X') {
            return Optional.of("La posicion 11 de la CURP (sexo) debe ser H, M o X");
        }

        String entidad = curp.substring(11, 13);
        if (!ENTIDADES.contains(entidad)) {
            return Optional.of("La clave de entidad federativa '" + entidad
                    + "' (posiciones 12 y 13 de la CURP) no existe. Ejemplos validos: GT, JC, DF, MC, NE (nacido en el extranjero)");
        }

        if (!CONSONANTES.matcher(curp.substring(13, 16)).matches()) {
            return Optional.of("Las posiciones 14 a 16 de la CURP deben ser consonantes");
        }

        char homoclave = curp.charAt(16);
        if (!Character.isLetterOrDigit(homoclave)) {
            return Optional.of("La posicion 17 de la CURP (homoclave) debe ser letra o numero");
        }
        boolean nacioDespuesDel2000 = nacimiento.getYear() >= 2000;
        if (nacioDespuesDel2000 && !Character.isLetter(homoclave)) {
            return Optional.of("La posicion 17 de la CURP debe ser una letra para personas nacidas del 2000 en adelante");
        }
        if (!nacioDespuesDel2000 && !Character.isDigit(homoclave)) {
            return Optional.of("La posicion 17 de la CURP debe ser un numero para personas nacidas antes del 2000");
        }

        if (!Character.isDigit(curp.charAt(17))) {
            return Optional.of("La posicion 18 de la CURP (digito verificador) debe ser un numero");
        }
        if (VALIDAR_DIGITO_VERIFICADOR && Character.getNumericValue(curp.charAt(17)) != digitoVerificador(curp)) {
            return Optional.of("El digito verificador de la CURP es incorrecto (deberia ser "
                    + digitoVerificador(curp) + ")");
        }

        return Optional.empty();
    }

    public static boolean esValida(String curp) {
        return validar(curp).isEmpty();
    }

    /** Fecha de nacimiento contenida en una CURP ya validada en estructura. */
    public static LocalDate fechaNacimiento(String valor) {
        String curp = normalizar(valor);
        int anio = Integer.parseInt(curp.substring(4, 6));
        int mes = Integer.parseInt(curp.substring(6, 8));
        int dia = Integer.parseInt(curp.substring(8, 10));
        int siglo = Character.isDigit(curp.charAt(16)) ? 1900 : 2000;
        return LocalDate.of(siglo + anio, mes, dia);
    }

    /** Sexo (H, M o X) contenido en la CURP. */
    public static char sexo(String valor) {
        return normalizar(valor).charAt(10);
    }

    private static int digitoVerificador(String curp) {
        int suma = 0;
        for (int i = 0; i < 17; i++) {
            suma += DICCIONARIO.indexOf(curp.charAt(i)) * (18 - i);
        }
        return (10 - (suma % 10)) % 10;
    }
}