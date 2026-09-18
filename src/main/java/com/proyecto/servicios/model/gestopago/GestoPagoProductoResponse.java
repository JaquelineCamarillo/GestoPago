package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GestoPagoProductoResponse {
    private Integer idProducto;
    private Integer idServicio;
    private String nombreServicio;
    private String nombreProducto;
    private String precio;
}