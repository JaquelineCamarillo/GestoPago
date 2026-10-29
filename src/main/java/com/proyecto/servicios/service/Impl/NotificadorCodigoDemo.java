package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.service.NotificadorCodigo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementacion en modo demostracion de NotificadorCodigo:
 * registra el evento de envio para el cliente y devuelve el codigo en el campo codigoDemo
 * de la respuesta de POST /pagos/iniciar para facilitar las pruebas en Swagger UI.
 */
@Component
@Slf4j
public class NotificadorCodigoDemo implements NotificadorCodigo {

    @Override
    public String enviarCodigoConfirmacion(Cliente cliente, Integer pagoId, String codigo) {
        log.info("[MODO DEMO 2FA] Codigo de confirmacion generado para pagoId={}, clienteId={}, correo={}",
                pagoId, cliente.getId(), cliente.getCorreoElectronico());
        return codigo;
    }
}
