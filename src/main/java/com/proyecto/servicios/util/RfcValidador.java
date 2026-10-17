package com.proyecto.servicios.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador del RFC.
 *
 * Persona fisica (13 caracteres):  4 letras + fecha de nacimiento AAMMDD + homoclave (2) + digito verificador.
 * Persona moral  (12 caracteres):  3 letras (admite &) + fecha de constitucion AAMMDD + homoclave (2) + digito verificador.
 *
 * El digito verificador es un numero del 0 al 9 o la letra A. Los RFC genericos del SAT
 * (XAXX010101000 publico en general y XEXX010101000 extranjeros) se aceptan como persona fisica.
 */
public final class RfcValidador {

    /** Tipo de persona esperado. */
    public enum Tipo { FISICA, MORAL, CUALQUIERA }

    /** Poner en false si se quiere aceptar RFC con digito verificador incorrecto. */
    public static final boolean VALIDAR_DIGITO_VERIFICADOR = true;

    private static final Set<String> GENERICOS = Set.of("XAXX010101000", "XEXX010101000");

    // \u00D1 = Ñ (con escape para evitar problemas de codificacion en Windows)
    private static final String DICCIONARIO = "0123456789ABCDEFGHIJKLMN&OPQRSTUVWXYZ \u00D1";

    private static final Pattern CARACTERES_VALIDOS = Pattern.compile("^[A-Z0-9\u00D1&]+$");
    private static final Pattern LETRAS_FISICA = Pattern.compile("^[A-Z\u00D1][AEIOUX][A-Z\u00D1]{2}$");
    private static final Pattern LETRAS_MORAL = Pattern.compile("^[A-Z\u00D1&]{3}$");
    private static final Pattern HOMOCLAVE = Pattern.compile("^[A-Z0-9]{2}$");

    private RfcValidador() {
    }

    /** Normaliza: quita espacios de los extremos y pasa a mayusculas. */
    public static String normalizar(String rfc) {
        return rfc == null ? null : rfc.trim().toUpperCase();
    }

    /** Valida como persona fisica (13 caracteres). */
    public static Optional<String> validar(String valor) {
        return validar(valor, Tipo.FISICA);
    }

    /** Devuelve el mensaje del primer error encontrado, o vacio si el RFC es valido para el tipo indicado. */
    public static Optional<String> validar(String valor, Tipo tipo) {
        if (valor == null || valor.isBlank()) {
            return Optional.of("El RFC es obligatorio y no puede estar vacio ni contener solo espacios");
        }
        String rfc = normalizar(valor);

        if (rfc.chars().anyMatch(Character::isWhitespace)) {
            return Optional.of("El RFC no puede contener espacios entre sus caracteres");
        }
        if (!CARACTERES_VALIDOS.matcher(rfc).matches()) {
            return Optional.of("El RFC solo puede contener letras y numeros, sin acentos ni simbolos "
                    + "(en persona moral tambien se permite el caracter &)");
        }

        // RFC genericos del SAT
        if (GENERICOS.contains(rfc)) {
            return tipo == Tipo.MORAL
                    ? Optional.of("El RFC generico " + rfc + " corresponde a una persona fisica, no a una persona moral")
                    : Optional.empty();
        }

        Optional<String> errorLongitud = validarLongitud(rfc, tipo);
        if (errorLongitud.isPresent()) {
            return errorLongitud;
        }

        boolean moral = rfc.length() == 12;
        String persona = moral ? "persona moral" : "persona fisica";
        String fechaEtiqueta = moral ? "fecha de constitucion" : "fecha de nacimiento";
        int inicioFecha = moral ? 3 : 4;

        String letras = rfc.substring(0, inicioFecha);
        if (moral) {
            if (!LETRAS_MORAL.matcher(letras).matches()) {
                return Optional.of("Las posiciones 1 a 3 del RFC de " + persona + " deben ser letras (se permite &)");
            }
        } else if (!LETRAS_FISICA.matcher(letras).matches()) {
            return Optional.of("Las posiciones 1 a 4 del RFC de " + persona
                    + " deben ser letras y la segunda debe ser vocal o X");
        }

        String fecha = rfc.substring(inicioFecha, inicioFecha + 6);
        if (!fecha.chars().allMatch(Character::isDigit)) {
            return Optional.of("Las posiciones " + (inicioFecha + 1) + " a " + (inicioFecha + 6)
                    + " del RFC deben ser la " + fechaEtiqueta + " en formato AAMMDD (solo numeros)");
        }
        if (fechaEn(rfc, inicioFecha).isEmpty()) {
            return Optional.of("La " + fechaEtiqueta + " dentro del RFC (" + fecha + ") no es una fecha valida");
        }

        if (!HOMOCLAVE.matcher(rfc.substring(inicioFecha + 6, inicioFecha + 8)).matches()) {
            return Optional.of("Las posiciones " + (inicioFecha + 7) + " y " + (inicioFecha + 8)
                    + " del RFC (homoclave) deben ser letras o numeros");
        }

        char ultimo = rfc.charAt(rfc.length() - 1);
        if (!(Character.isDigit(ultimo) || ultimo == 'A')) {
            return Optional.of("La ultima posicion del RFC (digito verificador) debe ser un numero o la letra A");
        }
        if (VALIDAR_DIGITO_VERIFICADOR && ultimo != digitoVerificador(rfc)) {
            return Optional.of("El digito verificador del RFC es incorrecto (deberia ser "
                    + digitoVerificador(rfc) + ")");
        }

        return Optional.empty();
    }

    private static Optional<String> validarLongitud(String rfc, Tipo tipo) {
        int longitud = rfc.length();
        switch (tipo) {
            case FISICA:
                if (longitud == 12) {
                    return Optional.of("El RFC tiene 12 caracteres, que corresponde a una persona moral. "
                            + "Aqui se requiere el RFC de persona fisica de 13 caracteres");
                }
                if (longitud != 13) {
                    return Optional.of("El RFC de persona fisica debe tener exactamente 13 caracteres y tiene " + longitud);
                }
                return Optional.empty();
            case MORAL:
                if (longitud == 13) {
                    return Optional.of("El RFC tiene 13 caracteres, que corresponde a una persona fisica. "
                            + "Aqui se requiere el RFC de persona moral de 12 caracteres");
                }
                if (longitud != 12) {
                    return Optional.of("El RFC de persona moral debe tener exactamente 12 caracteres y tiene " + longitud);
                }
                return Optional.empty();
            default:
                if (longitud != 12 && longitud != 13) {
                    return Optional.of("El RFC debe tener 12 caracteres (persona moral) o 13 (persona fisica) y tiene " + longitud);
                }
                return Optional.empty();
        }
    }

    public static boolean esValido(String rfc) {
        return validar(rfc).isEmpty();
    }

    public static boolean esValido(String rfc, Tipo tipo) {
        return validar(rfc, tipo).isEmpty();
    }

    /** true si el RFC (ya valido) es de persona moral (12 caracteres). */
    public static boolean esPersonaMoral(String rfc) {
        return normalizar(rfc).length() == 12;
    }

    /**
     * Fecha de nacimiento contenida en un RFC de persona fisica. El RFC no indica el siglo,
     * asi que se toma el mas reciente que no sea futuro. Vacio si la fecha no existe.
     */
    public static Optional<LocalDate> fechaNacimiento(String valor) {
        return fechaEn(normalizar(valor), 4);
    }

    private static Optional<LocalDate> fechaEn(String rfc, int inicio) {
        int anio = Integer.parseInt(rfc.substring(inicio, inicio + 2));
        int mes = Integer.parseInt(rfc.substring(inicio + 2, inicio + 4));
        int dia = Integer.parseInt(rfc.substring(inicio + 4, inicio + 6));
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

    /** Persona moral: se antepone un espacio para completar las 12 posiciones del calculo. */
    private static char digitoVerificador(String rfc) {
        String base = rfc.length() == 12 ? " " + rfc.substring(0, 11) : rfc.substring(0, 12);
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            suma += DICCIONARIO.indexOf(base.charAt(i)) * (13 - i);
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