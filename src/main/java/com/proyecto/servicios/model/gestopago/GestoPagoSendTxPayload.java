package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Estructura JSON que se cifra en AES-256-CBC y se envia en el parametro "signed"
 * a POST /sistema/service/sendTx.do y POST /sistema/service/confirmTx.do
 * segun la documentacion oficial de GestoPago/PuntoRed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoSendTxPayload {

    private Integer idProducto;
    private Integer idServicio;
    private String telefono;
    private String horaLocal;
    private String referencia;
    private BigDecimal montoPago;
    private String upc;
    private String unidad;
}
