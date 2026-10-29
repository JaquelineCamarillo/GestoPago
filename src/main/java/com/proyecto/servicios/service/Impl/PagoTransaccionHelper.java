package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Login;
import com.proyecto.servicios.entity.sf.Pago;
import com.proyecto.servicios.entity.sf.PagoBitacora;
import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.enums.TipoPago;
import com.proyecto.servicios.exception.pagos.CodigoConfirmacionExpiradoException;
import com.proyecto.servicios.exception.pagos.CodigoConfirmacionInvalidoException;
import com.proyecto.servicios.exception.pagos.EstadoPagoInvalidoException;
import com.proyecto.servicios.exception.pagos.IntentosCodigoAgotadosException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.PagoNoEncontradoException;
import com.proyecto.servicios.exception.pagos.SaldoInsuficienteException;
import com.proyecto.servicios.exception.pagos.SesionInvalidaException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago;
import com.proyecto.servicios.model.pagos.BitacoraPagoResponse;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.repositorys.sf.PagoBitacoraRepository;
import com.proyecto.servicios.repositorys.sf.PagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Componente auxiliar para ejecutar transacciones cortas e independientes (REQUIRES_NEW)
 * sobre pagos y cuentas con bloqueo pesimista (PESSIMISTIC_WRITE).
 * Garantiza que la retencion de saldo se confirme en base de datos ANTES de abrir la llamada HTTP
 * hacia GestoPago, de modo que nunca se mantenga una conexion o lock de BD durante llamadas de red.
 */
@Component
@Slf4j
public class PagoTransaccionHelper {

    public static final int MAX_INTENTOS_CODIGO = 3;
    private static final long MINUTOS_INACTIVIDAD_SESION = 5;
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TELEFONO_FIJO_SERVICIOS = "1111111111";

    private final PagoRepository pagoRepository;
    private final PagoBitacoraRepository bitacoraRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final LoginRepository loginRepository;
    private final TextEncryptor textEncryptor;

    public PagoTransaccionHelper(PagoRepository pagoRepository,
                                 PagoBitacoraRepository bitacoraRepository,
                                 CuentaRepository cuentaRepository,
                                 ClienteRepository clienteRepository,
                                 LoginRepository loginRepository,
                                 TextEncryptor textEncryptor) {
        this.pagoRepository = pagoRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.loginRepository = loginRepository;
        this.textEncryptor = textEncryptor;
    }

    /**
     * Valida que el cliente tenga sesion activa, no expirada por inactividad (>5 min),
     * y que el tokenSesion coincida exactamente con el token cifrado de su sesion.
     * Actualiza ultimaActividad para mantener viva la sesion.
     */
    @Transactional
    public void validarYRefrescarSesion(Integer clienteId, String tokenSesion) {
        Login login = loginRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new SesionInvalidaException("El cliente no tiene una sesion iniciada"));

        if (!Boolean.TRUE.equals(login.getActivo()) || login.getTokenCifrado() == null) {
            throw new SesionInvalidaException("La sesion del cliente no esta activa. Inicie sesion nuevamente");
        }

        LocalDateTime limiteInactividad = LocalDateTime.now().minusMinutes(MINUTOS_INACTIVIDAD_SESION);
        if (login.getUltimaActividad() == null || login.getUltimaActividad().isBefore(limiteInactividad)) {
            login.setActivo(false);
            login.setTokenCifrado(null);
            loginRepository.save(login);
            throw new SesionInvalidaException("La sesion expiro por inactividad (mas de 5 minutos). Inicie sesion nuevamente");
        }

        String tokenGuardado;
        try {
            tokenGuardado = textEncryptor.decrypt(login.getTokenCifrado());
        } catch (Exception e) {
            throw new SesionInvalidaException("El token de sesion almacenado no pudo verificarse. Inicie sesion nuevamente");
        }

        boolean coincide = MessageDigest.isEqual(
                tokenGuardado.getBytes(StandardCharsets.UTF_8),
                tokenSesion.trim().getBytes(StandardCharsets.UTF_8));
        if (!coincide) {
            throw new SesionInvalidaException("El tokenSesion no corresponde a la sesion activa del cliente");
        }

        login.setUltimaActividad(LocalDateTime.now());
        loginRepository.save(login);
    }

    /**
     * Transaccion corta (REQUIRES_NEW) que bloquea el pago y la cuenta con PESSIMISTIC_WRITE,
     * verifica estado, vigencia e intentos del codigo de 6 digitos, revalida cliente y cuenta activos
     * y saldo suficiente, y RETIENE el monto (descontandolo del saldo disponible e incrementando saldoRetenido)
     * dejando el pago en estado EN_PROCESO antes de salir a red.
     * Si el codigo es invalido, expirado o agoto intentos, registra el cambio y lanza un RuntimeException
     * configurado con noRollbackFor para que el incremento de intentos o cambio a EXPIRADO/RECHAZADO
     * quede confirmado en base de datos.
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {
                    CodigoConfirmacionInvalidoException.class,
                    CodigoConfirmacionExpiradoException.class,
                    IntentosCodigoAgotadosException.class
            }
    )
    public GestoPagoSendTxPayload validarCodigoYRetenerSaldo(Integer pagoId, String tokenSesion, String codigoEnviado) {
        Pago pago = pagoRepository.findByIdForUpdate(pagoId)
                .orElseThrow(() -> new PagoNoEncontradoException(pagoId));

        Cliente cliente = clienteRepository.findById(pago.getCliente().getId())
                .orElseThrow(() -> new OperacionNoAutorizadaException("El cliente asociado al pago no existe"));

        validarYRefrescarSesion(cliente.getId(), tokenSesion);

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new OperacionNoAutorizadaException("El cliente titular se encuentra dado de baja y no puede operar");
        }

        if (pago.getEstado() != EstadoPago.PENDIENTE_CONFIRMACION) {
            throw new EstadoPagoInvalidoException("El pago con id " + pagoId + " ya fue procesado o no admite confirmacion (estado actual: "
                    + pago.getEstado() + ")");
        }

        if (pago.getIntentosCodigo() >= MAX_INTENTOS_CODIGO) {
            cambiarEstado(pago, EstadoPago.RECHAZADO, "Pago rechazado por superar el maximo de 3 intentos de codigo");
            throw new IntentosCodigoAgotadosException("Se agotaron los 3 intentos permitidos para confirmar este pago");
        }

        if (LocalDateTime.now().isAfter(pago.getCodigoExpiracion())) {
            cambiarEstado(pago, EstadoPago.EXPIRADO, "El codigo de confirmacion de 6 digitos expiro (mas de 5 minutos)");
            throw new CodigoConfirmacionExpiradoException("El codigo de confirmacion ha expirado (vigencia de 5 minutos)");
        }

        String codigoReal;
        try {
            codigoReal = textEncryptor.decrypt(pago.getCodigoCifrado());
        } catch (Exception e) {
            throw new CodigoConfirmacionInvalidoException("No fue posible verificar el codigo de confirmacion");
        }

        boolean codigoCorrecto = MessageDigest.isEqual(
                codigoReal.getBytes(StandardCharsets.UTF_8),
                codigoEnviado.trim().getBytes(StandardCharsets.UTF_8));

        if (!codigoCorrecto) {
            int nuevosIntentos = pago.getIntentosCodigo() + 1;
            pago.setIntentosCodigo(nuevosIntentos);
            if (nuevosIntentos >= MAX_INTENTOS_CODIGO) {
                cambiarEstado(pago, EstadoPago.RECHAZADO,
                        "Codigo incorrecto en intento " + nuevosIntentos + " de " + MAX_INTENTOS_CODIGO + "; pago bloqueado");
                throw new IntentosCodigoAgotadosException(
                        "Codigo de confirmacion incorrecto. Se agotaron los 3 intentos permitidos y el pago fue rechazado");
            }
            pagoRepository.save(pago);
            registrarBitacora(pago, EstadoPago.PENDIENTE_CONFIRMACION, EstadoPago.PENDIENTE_CONFIRMACION,
                    "Intento fallido de codigo de confirmacion (" + nuevosIntentos + " de " + MAX_INTENTOS_CODIGO + ")");
            throw new CodigoConfirmacionInvalidoException("El codigo de confirmacion es incorrecto (intento "
                    + nuevosIntentos + " de " + MAX_INTENTOS_CODIGO + ")");
        }

        // Bloqueo pesimista sobre la cuenta antes de verificar y retener saldo
        Cuenta cuenta = cuentaRepository.findByIdForUpdate(pago.getCuenta().getId())
                .orElseThrow(() -> new OperacionNoAutorizadaException("La cuenta asociada al pago no existe"));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new OperacionNoAutorizadaException("La cuenta " + cuenta.getNumeroCuenta() + " no se encuentra ACTIVA");
        }

        if (!cuenta.getCliente().getId().equals(cliente.getId())) {
            throw new OperacionNoAutorizadaException("La cuenta no pertenece al cliente autenticado");
        }

        BigDecimal saldoActual = aBigDecimal(cuenta.getSaldo());
        BigDecimal retenidoActual = aBigDecimal(cuenta.getSaldoRetenido());
        BigDecimal montoPago = pago.getMonto().setScale(2, RoundingMode.HALF_UP);

        if (saldoActual.compareTo(montoPago) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta para cubrir el monto de "
                    + montoPago + " (saldo disponible: " + saldoActual + ")");
        }

        // Retencion: se descuenta del saldo disponible y se suma a saldo_retenido
        BigDecimal nuevoSaldoDisponible = saldoActual.subtract(montoPago).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevoSaldoRetenido = retenidoActual.add(montoPago).setScale(2, RoundingMode.HALF_UP);

        cuenta.setSaldo(nuevoSaldoDisponible.doubleValue());
        cuenta.setSaldoRetenido(nuevoSaldoRetenido);
        cuentaRepository.save(cuenta);

        LocalDateTime ahora = LocalDateTime.now();
        String horaLocal = ahora.format(FORMATO_HORA_LOCAL);
        pago.setHoraLocal(horaLocal);
        pago.setFechaConfirmacion(ahora);
        pago.setMontoRetenido(true);
        cambiarEstado(pago, EstadoPago.EN_PROCESO,
                "Segunda verificacion exitosa; monto retenido (" + montoPago + ") antes de llamar al proveedor");

        log.info("Retencion aplicada para pagoId={}, trace={}, monto={}, saldoDisponibleRestante={}",
                pago.getId(), pago.getTrace(), montoPago, nuevoSaldoDisponible);

        return construirPayloadProveedor(pago);
    }

    /**
     * Transaccion corta (REQUIRES_NEW) que aplica el resultado devuelto por el proveedor:
     * - EXITO -> convierte la retencion en cargo definitivo (descuenta de saldoRetenido, montoRetenido=false, COMPLETADO).
     * - RECHAZO_DEFINITIVO -> libera la retencion (devuelve monto a saldo disponible y resta de saldoRetenido, FALLIDO).
     * - REQUIERE_CONCILIACION -> mantiene la retencion intacta y deja el pago en EN_REVISION.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PagoResponse aplicarResultadoProveedor(Integer pagoId, ResultadoProveedorPago resultado) {
        Pago pago = pagoRepository.findByIdForUpdate(pagoId)
                .orElseThrow(() -> new PagoNoEncontradoException(pagoId));
        Cuenta cuenta = cuentaRepository.findByIdForUpdate(pago.getCuenta().getId())
                .orElseThrow(() -> new OperacionNoAutorizadaException("La cuenta asociada al pago no existe"));

        BigDecimal montoPago = pago.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoActual = aBigDecimal(cuenta.getSaldo());
        BigDecimal retenidoActual = aBigDecimal(cuenta.getSaldoRetenido());

        pago.setCodigoProveedor(resultado.getCodigoProveedor());
        pago.setMensajeProveedor(resultado.getMensajeProveedor());
        if (resultado.getIdTx() != null) {
            pago.setIdTxProveedor(resultado.getIdTx());
        }
        if (resultado.getNumAutorizacion() != null) {
            pago.setNumAutorizacion(resultado.getNumAutorizacion());
        }
        if (resultado.getPin() != null) {
            pago.setPinProveedor(resultado.getPin());
        }
        if (resultado.getLegend() != null) {
            pago.setLeyendaProveedor(resultado.getLegend());
        }

        switch (resultado.getEstado()) {
            case EXITO -> {
                if (Boolean.TRUE.equals(pago.getMontoRetenido())) {
                    BigDecimal nuevoRetenido = retenidoActual.subtract(montoPago).max(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                    cuenta.setSaldoRetenido(nuevoRetenido);
                    cuentaRepository.save(cuenta);
                    pago.setMontoRetenido(false);
                }
                cambiarEstado(pago, EstadoPago.COMPLETADO,
                        "Pago completado con exito ante el proveedor (codigo=" + resultado.getCodigoProveedor()
                                + ", aut=" + resultado.getNumAutorizacion() + "); retencion aplicada como cargo definitivo");
            }
            case RECHAZO_DEFINITIVO -> {
                if (Boolean.TRUE.equals(pago.getMontoRetenido())) {
                    BigDecimal nuevoSaldo = saldoActual.add(montoPago).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal nuevoRetenido = retenidoActual.subtract(montoPago).max(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                    cuenta.setSaldo(nuevoSaldo.doubleValue());
                    cuenta.setSaldoRetenido(nuevoRetenido);
                    cuentaRepository.save(cuenta);
                    pago.setMontoRetenido(false);
                }
                cambiarEstado(pago, EstadoPago.FALLIDO,
                        "Proveedor rechazo la transaccion (codigo=" + resultado.getCodigoProveedor()
                                + "): retencion liberada y saldo reembolsado a la cuenta");
            }
            case REQUIERE_CONCILIACION -> cambiarEstado(pago, EstadoPago.EN_REVISION,
                    "Sin confirmacion definitiva del proveedor (codigo=" + resultado.getCodigoProveedor()
                            + "): se mantiene la retencion y el pago pasa a EN_REVISION para conciliacion con confirmTx.do");
        }

        return construirResponse(pago, cuenta, null);
    }

    public GestoPagoSendTxPayload construirPayloadProveedor(Pago pago) {
        if (pago.getTipoPago() == TipoPago.RECARGA) {
            return GestoPagoSendTxPayload.builder()
                    .idProducto(pago.getIdProducto())
                    .idServicio(pago.getIdServicio())
                    .telefono(pago.getTelefono())
                    .horaLocal(pago.getHoraLocal())
                    .upc(pago.getTrace())
                    .unidad(pago.getUnidad())
                    .build();
        }
        return GestoPagoSendTxPayload.builder()
                .idProducto(pago.getIdProducto())
                .idServicio(pago.getIdServicio())
                .telefono(TELEFONO_FIJO_SERVICIOS)
                .horaLocal(pago.getHoraLocal())
                .referencia(pago.getReferencia())
                .montoPago(pago.getMonto().setScale(2, RoundingMode.HALF_UP))
                .upc(pago.getTrace())
                .unidad(pago.getUnidad())
                .build();
    }

    public void cambiarEstado(Pago pago, EstadoPago nuevoEstado, String detalle) {
        EstadoPago anterior = pago.getEstado();
        pago.setEstado(nuevoEstado);
        pagoRepository.save(pago);
        registrarBitacora(pago, anterior, nuevoEstado, detalle);
    }

    public void registrarBitacora(Pago pago, EstadoPago anterior, EstadoPago nuevo, String detalle) {
        PagoBitacora entrada = new PagoBitacora(pago, anterior, nuevo, detalle);
        bitacoraRepository.save(entrada);
    }

    @Transactional(readOnly = true)
    public PagoResponse construirResponse(Pago pago, Cuenta cuenta, String codigoDemo) {
        List<BitacoraPagoResponse> bitacora = bitacoraRepository
                .findByPagoIdOrderByFechaRegistroAscIdAsc(pago.getId())
                .stream()
                .map(b -> new BitacoraPagoResponse(
                        b.getEstadoAnterior(),
                        b.getEstadoNuevo(),
                        b.getDetalle(),
                        b.getFechaRegistro()))
                .collect(Collectors.toList());

        return PagoResponse.builder()
                .pagoId(pago.getId())
                .idUser(pago.getCliente().getId())
                .numeroCuenta(pago.getNumeroCuenta())
                .tipoPago(pago.getTipoPago())
                .idServicio(pago.getIdServicio())
                .idProd(pago.getIdProducto())
                .nombreServicio(pago.getNombreServicio())
                .nombreProducto(pago.getNombreProducto())
                .monto(pago.getMonto())
                .telefono(pago.getTelefono())
                .referencia(pago.getReferencia())
                .trace(pago.getTrace())
                .estado(pago.getEstado())
                .montoRetenido(pago.getMontoRetenido())
                .saldoDisponibleCuenta(aBigDecimal(cuenta.getSaldo()))
                .saldoRetenidoCuenta(aBigDecimal(cuenta.getSaldoRetenido()))
                .idTxProveedor(pago.getIdTxProveedor())
                .numAutorizacion(pago.getNumAutorizacion())
                .codigoProveedor(pago.getCodigoProveedor())
                .mensajeProveedor(pago.getMensajeProveedor())
                .pinProveedor(pago.getPinProveedor())
                .leyendaProveedor(pago.getLeyendaProveedor())
                .codigoDemo(codigoDemo)
                .codigoExpiracion(pago.getCodigoExpiracion())
                .fechaCreacion(pago.getFechaCreacion())
                .fechaConfirmacion(pago.getFechaConfirmacion())
                .fechaActualizacion(pago.getFechaActualizacion())
                .bitacora(bitacora)
                .build();
    }

    public static BigDecimal aBigDecimal(Double valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal aBigDecimal(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
