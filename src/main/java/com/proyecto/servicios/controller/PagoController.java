package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.pagos.ConfirmarPagoRequest;
import com.proyecto.servicios.model.pagos.ConsultarPagoRequest;
import com.proyecto.servicios.model.pagos.IniciarPagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/pagos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Pagos", description = "Pagos de servicios y recargas con GestoPago (estrategia retener -> cobrar -> conciliar)")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping(value = "/iniciar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Inicia una solicitud de pago o recarga y genera el codigo de confirmacion de 6 digitos (1a verificacion)")
    public ResponseEntity<GenericResponse> iniciarPago(@Valid @RequestBody IniciarPagoRequest request) {
        PagoResponse response = pagoService.iniciarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(
                        "Solicitud de pago iniciada en PENDIENTE_CONFIRMACION. Confirme con el codigo de 6 digitos",
                        response));
    }

    @PostMapping(value = "/confirmar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirma el pago con el codigo de 6 digitos (2a verificacion), retiene el saldo y ejecuta sendTx.do en GestoPago")
    public ResponseEntity<GenericResponse> confirmarPago(@Valid @RequestBody ConfirmarPagoRequest request) {
        PagoResponse response = pagoService.confirmarPago(request);

        if (response.getEstado() == EstadoPago.COMPLETADO) {
            return ResponseEntity.ok(GenericResponse.exito("Pago realizado y aplicado con exito", response));
        }

        if (response.getEstado() == EstadoPago.EN_REVISION) {
            GenericResponse body = new GenericResponse(
                    HttpStatus.GATEWAY_TIMEOUT.value(),
                    "El proveedor no respondio a tiempo; el monto permanece retenido y el pago quedo EN_REVISION para conciliacion",
                    response);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
        }

        GenericResponse body = new GenericResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "El pago fue rechazado por el proveedor (" + response.getMensajeProveedor()
                        + "); la retencion fue liberada y el saldo reembolsado a su cuenta",
                response);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta el estado actual y la bitacora de transiciones de un pago")
    public ResponseEntity<GenericResponse> consultarPago(@Valid @RequestBody ConsultarPagoRequest request) {
        PagoResponse response = pagoService.consultarPago(request);
        return ResponseEntity.ok(GenericResponse.exito("Consulta de pago realizada correctamente", response));
    }

    @PostMapping(value = "/conciliar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Concilia bajo demanda un pago en estado EN_REVISION consultando confirmTx.do en GestoPago")
    public ResponseEntity<GenericResponse> conciliarPago(@Valid @RequestBody ConsultarPagoRequest request) {
        PagoResponse response = pagoService.conciliarPago(request);
        return ResponseEntity.ok(GenericResponse.exito("Conciliacion de pago ejecutada correctamente", response));
    }
}
