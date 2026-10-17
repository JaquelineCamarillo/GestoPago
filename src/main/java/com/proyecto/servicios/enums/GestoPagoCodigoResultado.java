package com.proyecto.servicios.enums;

import org.springframework.http.HttpStatus;

/**
 * Codigos propios de la integracion con GestoPago. Convencion: 0 = exito; cualquier
 * valor distinto de 0 es un error especifico y clasificado (nunca un 400 generico).
 */
public enum GestoPagoCodigoResultado {
    EXITO(0, "Operacion realizada con exito", HttpStatus.OK),
    USUARIO_O_DISTRIBUIDOR_INVALIDO(1, "El idDistribuidor no es valido o no esta registrado en GestoPago", HttpStatus.UNAUTHORIZED),
    PASSWORD_INVALIDO(2, "La contrasena configurada para GestoPago es incorrecta", HttpStatus.UNAUTHORIZED),
    DISPOSITIVO_INVALIDO(3, "El codigoDispositivo no esta registrado para este distribuidor", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRADO(4, "El token de GestoPago expiro (24h) y debe renovarse", HttpStatus.UNAUTHORIZED),
    TOKEN_NO_DISPONIBLE(5, "No fue posible obtener un token valido de GestoPago", HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT(6, "GestoPago no respondio a tiempo", HttpStatus.GATEWAY_TIMEOUT),
    ERROR_COMUNICACION(7, "GestoPago no esta disponible actualmente", HttpStatus.BAD_GATEWAY),
    RESPUESTA_NO_EXITOSA(8, "GestoPago respondio una operacion no exitosa", HttpStatus.BAD_GATEWAY),
    ERROR_INTERNO(9, "Ocurrio un error inesperado", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int codigo;
    private final String mensajePorDefecto;
    private final HttpStatus httpStatus;

    GestoPagoCodigoResultado(int codigo, String mensajePorDefecto, HttpStatus httpStatus) {
        this.codigo = codigo;
        this.mensajePorDefecto = mensajePorDefecto;
        this.httpStatus = httpStatus;
    }

    public int getCodigo() { return codigo; }
    public String getMensajePorDefecto() { return mensajePorDefecto; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}