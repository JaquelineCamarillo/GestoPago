package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/** Nodo &lt;MENSAJE&gt; de la respuesta XML de GestoPago. CODIGO="01" significa exito. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoMensajeXml {

    @JacksonXmlProperty(localName = "CODIGO")
    private String codigo;

    @JacksonXmlProperty(localName = "TEXTO")
    private String texto;
}