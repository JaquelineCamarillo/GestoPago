package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

/** Mapea el nodo raiz &lt;RESPONSE&gt; de GET /sistema/service/getProductList.do */
@Data
@JacksonXmlRootElement(localName = "RESPONSE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductListXmlResponse {

    @JacksonXmlProperty(localName = "MENSAJE")
    private GestoPagoMensajeXml mensaje;

    @JacksonXmlElementWrapper(localName = "PRODUCTOS")
    @JacksonXmlProperty(localName = "producto")
    private List<GestoPagoProductoXml> productos;
}