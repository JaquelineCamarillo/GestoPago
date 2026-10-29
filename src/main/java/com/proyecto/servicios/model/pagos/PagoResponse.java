package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.enums.TipoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de las operaciones de pago (iniciar, confirmar, consultar y conciliar).
 * Nunca expone tokens, codigos cifrados ni llaves.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoResponse {

    private Integer pagoId;
    private Integer idUser;
    private String numeroCuenta;
    private TipoPago tipoPago;
    private Integer idServicio;
    private Integer idProd;
    private String nombreServicio;
    private String nombreProducto;
    private BigDecimal monto;
    private String telefono;
    private String referencia;
    private String trace;
    private EstadoPago estado;
    private Boolean montoRetenido;
    private BigDecimal saldoDisponibleCuenta;
    private BigDecimal saldoRetenidoCuenta;
    private String idTxProveedor;
    private String numAutorizacion;
    private String codigoProveedor;
    private String mensajeProveedor;
    private String pinProveedor;
    private String leyendaProveedor;

    /** Solo se incluye al iniciar el pago cuando NotificadorCodigo opera en modo demo. */
    private String codigoDemo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime codigoExpiracion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime fechaConfirmacion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime fechaActualizacion;

    private List<BitacoraPagoResponse> bitacora;
}
