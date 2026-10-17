package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estructura unica de respuesta de la API, tanto para exitos como para errores:
 * <pre>
 * exito : { "codigo": 0,   "mensaje": "Cliente registrado correctamente", "datos": { ... } }
 * error : { "codigo": 404, "mensaje": "No existe un cliente con id: 99" }
 * </pre>
 * Convencion: codigo 0 = exito; cualquier otro valor es un error. "datos" solo aparece cuando hay informacion
 * que devolver.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse {

    @Schema(description = "0 = exito; cualquier otro valor identifica un error", example = "0")
    private Integer codigo;

    @Schema(description = "Descripcion del resultado", example = "Operacion realizada con exito")
    private String mensaje;

    @Schema(description = "Informacion devuelta por la operacion (solo en respuestas exitosas)")
    private Object datos;

    public GenericResponse(Integer codigo, String mensaje, Object datos) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    /** Respuesta exitosa con informacion. */
    public static GenericResponse exito(String mensaje, Object datos) {
        return new GenericResponse(0, mensaje, datos);
    }

    /** Respuesta exitosa sin informacion adicional. */
    public static GenericResponse exito(String mensaje) {
        return new GenericResponse(0, mensaje, null);
    }
}