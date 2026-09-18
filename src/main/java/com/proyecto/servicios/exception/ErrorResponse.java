package com.proyecto.servicios.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** Cuerpo de error uniforme para toda la API: nunca expone stacktraces ni datos sensibles. */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final Integer codigo;
    private final String error;
    private final String mensaje;
    private final LocalDateTime timestamp;

    public ErrorResponse(Integer codigo, String error, String mensaje) {
        this(codigo, error, mensaje, LocalDateTime.now());
    }
}