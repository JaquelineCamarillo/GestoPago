package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Objeto unico que se guarda en Redis bajo la llave "gestopago:productos". */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GestoPagoCatalogoCacheDto {
    private List<GestoPagoProductoResponse> productos;
    private LocalDateTime fechaActualizacion;
}