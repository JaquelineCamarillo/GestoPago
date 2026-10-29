package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * Nodo &lt;MENSAJE&gt; dentro de la respuesta XML de sendTx.do y confirmTx.do.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoSendTxMensajeXml {

    @JacksonXmlProperty(localName = "CODIGO")
    private String codigo;

    @JacksonXmlProperty(localName = "TEXTO")
    private String texto;

    @JacksonXmlProperty(localName = "REFERENCIA")
    private String referencia;

    @JacksonXmlProperty(localName = "PIN")
    private String pin;

    @JacksonXmlProperty(localName = "legend")
    private String legend;

    /** En confirmTx.do el nodo ID_TX viene dentro de <MENSAJE>. */
    @JacksonXmlProperty(localName = "ID_TX")
    private String idTx;
}
