package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * Representa cada nodo &lt;producto&gt; dentro de &lt;PRODUCTOS&gt;. Los campos de
 * negocio vienen como ATRIBUTOS del XML (no como elementos hijos), excepto "legend".
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductoXml {

    @JacksonXmlProperty(isAttribute = true, localName = "servicio")
    private String servicio;

    @JacksonXmlProperty(isAttribute = true, localName = "producto")
    private String producto;

    @JacksonXmlProperty(isAttribute = true, localName = "idServicio")
    private Integer idServicio;

    @JacksonXmlProperty(isAttribute = true, localName = "idProducto")
    private Integer idProducto;

    @JacksonXmlProperty(isAttribute = true, localName = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @JacksonXmlProperty(isAttribute = true, localName = "tipoFront")
    private Integer tipoFront;

    @JacksonXmlProperty(isAttribute = true, localName = "tipoReferencia")
    private String tipoReferencia;

    @JacksonXmlProperty(isAttribute = true, localName = "precio")
    private String precio;

    @JacksonXmlProperty(localName = "legend")
    private String legend;
}