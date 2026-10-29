package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecto.servicios.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraPagoResponse {

    private EstadoPago estadoAnterior;
    private EstadoPago estadoNuevo;
    private String detalle;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime fechaRegistro;
}
