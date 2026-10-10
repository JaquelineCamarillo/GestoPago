package com.proyecto.servicios.validation;

import com.proyecto.servicios.util.RfcValidador;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Valida que el texto sea un RFC de persona fisica bien formado (13 caracteres).
 * Un valor nulo o vacio se considera valido aqui; para exigirlo se combina con @NotBlank.
 */
@Documented
@Constraint(validatedBy = Rfc.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Rfc {

    String message() default "El RFC no es valido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<Rfc, String> {

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            if (valor == null || valor.isBlank()) {
                return true;
            }
            Optional<String> error = RfcValidador.validar(valor);
            if (error.isEmpty()) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(error.get()).addConstraintViolation();
            return false;
        }
    }
}