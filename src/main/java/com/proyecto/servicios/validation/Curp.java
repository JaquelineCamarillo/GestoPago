package com.proyecto.servicios.validation;

import com.proyecto.servicios.util.CurpValidador;
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
 * Valida que el texto sea una CURP bien formada.
 * - Un valor nulo se considera valido aqui (se exige con @NotBlank o con un parametro obligatorio).
 * - Una cadena vacia o solo con espacios NO es valida.
 */
@Documented
@Constraint(validatedBy = Curp.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Curp {

    String message() default "La CURP no es valida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<Curp, String> {

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            if (valor == null) {
                return true;
            }
            Optional<String> error = CurpValidador.validar(valor);
            if (error.isEmpty()) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(error.get()).addConstraintViolation();
            return false;
        }
    }
}