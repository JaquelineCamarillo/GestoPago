package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que el numero de cuenta tenga EXACTAMENTE 16 digitos (solo numeros, ni mas ni menos).
 * - Un valor nulo se considera valido aqui (se exige con @NotBlank cuando es obligatorio).
 * - Una cadena vacia, con espacios, con letras o de otra longitud NO es valida.
 */
@Documented
@Constraint(validatedBy = NumeroCuenta.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NumeroCuenta {

    int LONGITUD = 16;

    String message() default "El numero de cuenta debe tener exactamente 16 digitos, solo numeros, sin espacios ni letras";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<NumeroCuenta, String> {

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || esValido(valor);
        }

        /** true si el texto son exactamente 16 caracteres y todos son digitos 0-9 (ASCII). */
        public static boolean esValido(String valor) {
            if (valor == null || valor.length() != LONGITUD) {
                return false;
            }
            for (int i = 0; i < valor.length(); i++) {
                char c = valor.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            return true;
        }
    }
}