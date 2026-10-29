package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * Mapea el nodo raiz &lt;RESPONSE&gt; de POST /sistema/service/sendTx.do y POST /sistema/service/confirmTx.do.
 */
@Data
@JacksonXmlRootElement(localName = "RESPONSE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoSendTxXmlResponse {

    @JacksonXmlProperty(localName = "ID_TX")
    private String idTx;

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "SALDO")
    private String saldo;

    @JacksonXmlProperty(localName = "COMISION")
    private String comision;

    @JacksonXmlProperty(localName = "SALDO_F")
    private String saldoFormateado;

    @JacksonXmlProperty(localName = "COMISION_F")
    private String comisionFormateada;

    @JacksonXmlProperty(localName = "FECHA")
    private String fecha;

    @JacksonXmlProperty(localName = "MONTO")
    private String monto;

    @JacksonXmlProperty(localName = "MENSAJE")
    private GestoPagoSendTxMensajeXml mensaje;
}
