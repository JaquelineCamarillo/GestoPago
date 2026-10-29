package com.proyecto.servicios.util;

import java.security.SecureRandom;

/**
 * Genera numeros de cuenta de exactamente 16 digitos (solo numeros). El primero nunca es 0, para
 * que el numero conserve siempre sus 16 cifras si algun dia se maneja como numero.
 */
public final class GeneradorNumeroCuenta {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LONGITUD = 16;

    private GeneradorNumeroCuenta() {
    }

    public static String generar() {
        StringBuilder numero = new StringBuilder(LONGITUD);
        numero.append(1 + RANDOM.nextInt(9));
        for (int i = 1; i < LONGITUD; i++) {
            numero.append(RANDOM.nextInt(10));
        }
        return numero.toString();
    }
}