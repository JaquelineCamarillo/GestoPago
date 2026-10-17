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
 * Valida que el texto sea un RFC bien formado: longitud, estructura de persona fisica o moral,
 * fecha, caracteres permitidos y digito verificador.
 *
 * - Un valor nulo se considera valido aqui (se exige con @NotBlank o con un parametro obligatorio).
 * - Una cadena vacia o solo con espacios NO es valida.
 * - El atributo tipo indica si se espera persona fisica (13), moral (12) o cualquiera de las dos.
 */
@Documented
@Constraint(validatedBy = Rfc.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Rfc {

    String message() default "El RFC no es valido";

    RfcValidador.Tipo tipo() default RfcValidador.Tipo.FISICA;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<Rfc, String> {

        private RfcValidador.Tipo tipo = RfcValidador.Tipo.FISICA;

        @Override
        public void initialize(Rfc anotacion) {
            this.tipo = anotacion.tipo();
        }

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            if (valor == null) {
                return true;
            }
            Optional<String> error = RfcValidador.validar(valor, tipo);
            if (error.isEmpty()) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(error.get()).addConstraintViolation();
            return false;
        }
    }
}