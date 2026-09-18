package com.proyecto.servicios.exception.gestopago;

import lombok.Getter;

/**
 * Se lanza cuando GestoPago rechaza la autenticacion o el token es invalido/expirado.
 * Incluye el Motivo especifico para no responder un error generico.
 */
@Getter
public class GestoPagoAuthenticationException extends GestoPagoIntegrationException {

    public enum Motivo {
        USUARIO_O_DISTRIBUIDOR_INVALIDO("El idDistribuidor no es valido o no esta registrado en GestoPago"),
        PASSWORD_INVALIDO("La contrasena configurada para GestoPago es incorrecta"),
        DISPOSITIVO_INVALIDO("El codigoDispositivo no esta registrado para este distribuidor"),
        TOKEN_EXPIRADO("El token de GestoPago expiro (vigencia de 24 horas) y debe renovarse"),
        DESCONOCIDO("GestoPago rechazo la autenticacion por un motivo no identificado");

        private final String descripcion;

        Motivo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    private final Motivo motivo;

    public GestoPagoAuthenticationException(Motivo motivo) {
        super(motivo.getDescripcion());
        this.motivo = motivo;
    }

    public GestoPagoAuthenticationException(Motivo motivo, Throwable cause) {
        super(motivo.getDescripcion(), cause);
        this.motivo = motivo;
    }
}