package com.proyecto.servicios.validation;

import com.proyecto.servicios.service.NacionalidadService;
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
 * Valida que el texto sea un codigo de pais de 2 letras (MX, US, ES...) existente
 * en el catalogo de GET /catalogos/nacionalidades.
 * Un valor nulo o vacio se considera valido aqui; para exigirlo se combina con @NotBlank.
 */
@Documented
@Constraint(validatedBy = CodigoPais.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodigoPais {

    String message() default "El codigo de pais no existe en el catalogo. "
            + "Use un codigo de 2 letras de GET /catalogos/nacionalidades (ejemplo: MX)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CodigoPais, String> {

        private final NacionalidadService nacionalidadService;

        public Validador(NacionalidadService nacionalidadService) {
            this.nacionalidadService = nacionalidadService;
        }

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            if (valor == null || valor.isBlank()) {
                return true;
            }
            return nacionalidadService.esCodigoValido(valor);
        }
    }
}