package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProveedorPagoClient;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Pago;
import com.proyecto.servicios.enums.EstadoPago;
import com.proyecto.servicios.enums.TipoPago;
import com.proyecto.servicios.exception.clientes.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.clientes.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.pagos.EstadoPagoInvalidoException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.PagoDuplicadoException;
import com.proyecto.servicios.exception.pagos.PagoNoEncontradoException;
import com.proyecto.servicios.exception.pagos.ProductoCatalogoException;
import com.proyecto.servicios.exception.pagos.SaldoInsuficienteException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.ResultadoProveedorPago;
import com.proyecto.servicios.model.pagos.ConfirmarPagoRequest;
import com.proyecto.servicios.model.pagos.ConsultarPagoRequest;
import com.proyecto.servicios.model.pagos.IniciarPagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.PagoRepository;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.NotificadorCodigo;
import com.proyecto.servicios.service.PagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Implementacion del flujo de pagos con GestoPago bajo la estrategia retener -> cobrar -> conciliar:
 * 1) POST /pagos/iniciar: valida identidad/sesion, cliente activo, cuenta activa y propia, catalogo
 *    (existencia de idServicio+idProd y monto acorde al precio del catalogo), saldo suficiente y
 *    ausencia de solicitudes duplicadas; genera trace de 12 caracteres y codigo 2FA de 6 digitos.
 * 2) POST /pagos/confirmar: verifica el codigo de 6 digitos y retiene el monto en una transaccion corta
 *    (PESSIMISTIC_WRITE) ANTES de invocar al proveedor (fuera de transaccion de BD).
 * 3) Conciliacion automatica (@Scheduled) y bajo demanda via confirmTx.do para pagos EN_REVISION.
 */
@Service
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALFABETO_TRACE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LONGITUD_TRACE = 12;
    private static final long MINUTOS_VIGENCIA_CODIGO = 5;
    private static final long MINUTOS_VENTANA_DUPLICADOS = 15;
    private static final long SEGUNDOS_MINIMOS_CONCILIACION = 62;
    private static final long HORAS_MAXIMAS_CONCILIACION = 24;
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final PagoRepository pagoRepository;
    private final GestoPagoProductService gestoPagoProductService;
    private final ProveedorPagoClient proveedorPagoClient;
    private final NotificadorCodigo notificadorCodigo;
    private final TextEncryptor textEncryptor;
    private final PagoTransaccionHelper transaccionHelper;
    private final String unidadComercial;

    public PagoServiceImpl(ClienteRepository clienteRepository,
                           CuentaRepository cuentaRepository,
                           PagoRepository pagoRepository,
                           GestoPagoProductService gestoPagoProductService,
                           ProveedorPagoClient proveedorPagoClient,
                           NotificadorCodigo notificadorCodigo,
                           TextEncryptor textEncryptor,
                           PagoTransaccionHelper transaccionHelper,
                           @Value("${gestopago.pagos.unidad:083}") String unidadComercial) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
        this.gestoPagoProductService = gestoPagoProductService;
        this.proveedorPagoClient = proveedorPagoClient;
        this.notificadorCodigo = notificadorCodigo;
        this.textEncryptor = textEncryptor;
        this.transaccionHelper = transaccionHelper;
        this.unidadComercial = unidadComercial;
    }

    // ----------------------------------------------------------------- 1. Iniciar

    @Override
    @Transactional
    public PagoResponse iniciarPago(IniciarPagoRequest request) {
        Cliente cliente = clienteRepository.findById(request.getIdUser())
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + request.getIdUser()));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new OperacionNoAutorizadaException("El cliente con id " + cliente.getId()
                    + " se encuentra dado de baja y no esta autorizado para realizar pagos");
        }

        // 1a verificacion: sesion activa y token de sesion del cliente
        transaccionHelper.validarYRefrescarSesion(cliente.getId(), request.getTokenSesion());

        String numeroCuenta = request.getNumeroCuenta().trim();
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        if (!cuenta.getCliente().getId().equals(cliente.getId())) {
            throw new OperacionNoAutorizadaException("La cuenta " + numeroCuenta
                    + " no pertenece al cliente con id " + cliente.getId());
        }

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new OperacionNoAutorizadaException("La cuenta " + numeroCuenta
                    + " se encuentra INACTIVA y no permite realizar operaciones");
        }

        // Validacion contra el catalogo de GestoPago (idServicio + idProd y reglas de monto/tipoFront)
        GestoPagoProductoResponse producto = validarProductoYMonto(
                request.getIdServicio(), request.getIdProd(), request.getTipoPago(), request.getMonto());

        BigDecimal monto = request.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoDisponible = PagoTransaccionHelper.aBigDecimal(cuenta.getSaldo());
        if (saldoDisponible.compareTo(monto) < 0) {
            throw new SaldoInsuficienteException("Saldo disponible insuficiente en la cuenta " + numeroCuenta
                    + " para realizar el pago por " + monto + " (saldo disponible: " + saldoDisponible + ")");
        }

        String telefono = request.getTipoPago() == TipoPago.RECARGA ? request.getTelefono().trim() : null;
        String referencia = request.getTipoPago() == TipoPago.SERVICIO
                ? request.getReferencia().trim().toUpperCase(Locale.ROOT) : null;

        // Prevencion de solicitudes duplicadas en ventana de tiempo
        LocalDateTime limiteVentana = LocalDateTime.now().minusMinutes(MINUTOS_VENTANA_DUPLICADOS);
        List<EstadoPago> estadosActivos = List.of(
                EstadoPago.PENDIENTE_CONFIRMACION,
                EstadoPago.EN_PROCESO,
                EstadoPago.EN_REVISION
        );
        boolean existeDuplicado = pagoRepository.existeSolicitudDuplicadaEnVentana(
                cuenta.getId(), request.getIdServicio(), request.getIdProd(),
                monto, telefono, referencia, estadosActivos, limiteVentana);
        if (existeDuplicado) {
            throw new PagoDuplicadoException("Ya existe una solicitud de pago identica en curso o pendiente de confirmacion "
                    + "para la misma cuenta, producto, destino y monto en los ultimos " + MINUTOS_VENTANA_DUPLICADOS + " minutos");
        }

        String trace = generarTraceUnico();
        String codigoPlano = generarCodigoSeisDigitos();
        LocalDateTime ahora = LocalDateTime.now();

        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setCuenta(cuenta);
        pago.setNumeroCuenta(numeroCuenta);
        pago.setTipoPago(request.getTipoPago());
        pago.setIdServicio(request.getIdServicio());
        pago.setIdProducto(request.getIdProd());
        pago.setNombreServicio(producto.getNombreServicio());
        pago.setNombreProducto(producto.getNombreProducto());
        pago.setMonto(monto);
        pago.setTelefono(telefono);
        pago.setReferencia(referencia);
        pago.setTrace(trace);
        pago.setHoraLocal(ahora.format(FORMATO_HORA_LOCAL));
        pago.setUnidad(unidadComercial);
        pago.setEstado(EstadoPago.PENDIENTE_CONFIRMACION);
        pago.setCodigoCifrado(textEncryptor.encrypt(codigoPlano));
        pago.setCodigoExpiracion(ahora.plusMinutes(MINUTOS_VIGENCIA_CODIGO));
        pago.setIntentosCodigo(0);
        pago.setMontoRetenido(false);

        pago = pagoRepository.save(pago);
        transaccionHelper.registrarBitacora(pago, null, EstadoPago.PENDIENTE_CONFIRMACION,
                "Solicitud de pago iniciada (trace=" + trace + "); codigo de 6 digitos generado con vigencia de 5 minutos");

        String codigoDemo = notificadorCodigo.enviarCodigoConfirmacion(cliente, pago.getId(), codigoPlano);

        log.info("Pago iniciado id={}, clienteId={}, cuenta={}, trace={}, monto={}, estado={}",
                pago.getId(), cliente.getId(), numeroCuenta, trace, monto, pago.getEstado());

        return transaccionHelper.construirResponse(pago, cuenta, codigoDemo);
    }

    // --------------------------------------------------------------- 2. Confirmar

    /**
     * NO lleva @Transactional a proposito:
     * 1) transaccionHelper.validarCodigoYRetenerSaldo(...) corre en su propia transaccion corta (REQUIRES_NEW)
     *    con bloqueo pesimista (PESSIMISTIC_WRITE) y hace commit en base de datos ANTES de salir a la red.
     * 2) La llamada HTTP a GestoPago (proveedorPagoClient.enviarTransaccion) ocurre FUERA de cualquier
     *    transaccion o lock de base de datos.
     * 3) transaccionHelper.aplicarResultadoProveedor(...) corre en otra transaccion corta (REQUIRES_NEW)
     *    para reflejar el estado final (COMPLETADO, FALLIDO o EN_REVISION).
     */
    @Override
    public PagoResponse confirmarPago(ConfirmarPagoRequest request) {
        GestoPagoSendTxPayload payload = transaccionHelper.validarCodigoYRetenerSaldo(
                request.getPagoId(), request.getTokenSesion(), request.getCodigo());

        ResultadoProveedorPago resultado = proveedorPagoClient.enviarTransaccion(payload);

        return transaccionHelper.aplicarResultadoProveedor(request.getPagoId(), resultado);
    }

    // --------------------------------------------------------------- 3. Consultar

    @Override
    @Transactional
    public PagoResponse consultarPago(ConsultarPagoRequest request) {
        Pago pago = pagoRepository.findById(request.getPagoId())
                .orElseThrow(() -> new PagoNoEncontradoException(request.getPagoId()));

        transaccionHelper.validarYRefrescarSesion(pago.getCliente().getId(), request.getTokenSesion());

        // Si el pago seguia en PENDIENTE_CONFIRMACION y ya vencio su codigo, se marca EXPIRADO al consultarlo
        if (pago.getEstado() == EstadoPago.PENDIENTE_CONFIRMACION
                && LocalDateTime.now().isAfter(pago.getCodigoExpiracion())) {
            transaccionHelper.cambiarEstado(pago, EstadoPago.EXPIRADO,
                    "El codigo de confirmacion expiro sin ser confirmado dentro de los 5 minutos");
        }

        Cuenta cuenta = cuentaRepository.findById(pago.getCuenta().getId())
                .orElseThrow(() -> new CuentaNoEncontradaException(pago.getNumeroCuenta()));

        return transaccionHelper.construirResponse(pago, cuenta, null);
    }

    // --------------------------------------------------------------- 4. Conciliar

    @Override
    public PagoResponse conciliarPago(ConsultarPagoRequest request) {
        Pago pago = pagoRepository.findById(request.getPagoId())
                .orElseThrow(() -> new PagoNoEncontradoException(request.getPagoId()));

        transaccionHelper.validarYRefrescarSesion(pago.getCliente().getId(), request.getTokenSesion());

        if (pago.getEstado() != EstadoPago.EN_REVISION) {
            throw new EstadoPagoInvalidoException("Solo se pueden conciliar pagos en estado EN_REVISION (estado actual: "
                    + pago.getEstado() + ")");
        }

        GestoPagoSendTxPayload payload = transaccionHelper.construirPayloadProveedor(pago);
        ResultadoProveedorPago resultado = proveedorPagoClient.confirmarTransaccion(payload);
        return transaccionHelper.aplicarResultadoProveedor(pago.getId(), resultado);
    }

    /**
     * Job programado que revisa los pagos que quedaron EN_REVISION tras un timeout o fallo de red
     * y los concilia contra confirmTx.do respetando la ventana oficial de GestoPago
     * (despues de 62 segundos desde el envio de sendTx y hasta un maximo de 24 horas).
     */
    @Override
    @Scheduled(cron = "${gestopago.pagos.conciliacion-cron:0 */2 * * * *}")
    public void conciliarPagosEnRevision() {
        List<Pago> pendientes = pagoRepository.findByEstadoOrderByFechaCreacionAsc(EstadoPago.EN_REVISION);
        if (pendientes.isEmpty()) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (Pago pago : pendientes) {
            try {
                LocalDateTime referenciaTiempo = pago.getFechaConfirmacion() != null
                        ? pago.getFechaConfirmacion() : pago.getFechaCreacion();
                long segundosTranscurridos = Duration.between(referenciaTiempo, ahora).getSeconds();

                if (segundosTranscurridos < SEGUNDOS_MINIMOS_CONCILIACION) {
                    continue;
                }
                if (Duration.between(referenciaTiempo, ahora).toHours() >= HORAS_MAXIMAS_CONCILIACION) {
                    log.warn("Pago id={} trace={} supero las 24h de vigencia de confirmTx.do; requiere conciliacion manual",
                            pago.getId(), pago.getTrace());
                    continue;
                }

                GestoPagoSendTxPayload payload = transaccionHelper.construirPayloadProveedor(pago);
                ResultadoProveedorPago resultado = proveedorPagoClient.confirmarTransaccion(payload);
                if (resultado.getEstado() != ResultadoProveedorPago.EstadoResultado.REQUIERE_CONCILIACION) {
                    transaccionHelper.aplicarResultadoProveedor(pago.getId(), resultado);
                    log.info("Conciliacion automatica completada para pagoId={}, trace={}, resultado={}",
                            pago.getId(), pago.getTrace(), resultado.getEstado());
                }
            } catch (Exception e) {
                log.error("Error durante la conciliacion automatica del pagoId={}: {}", pago.getId(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------- Validaciones

    private GestoPagoProductoResponse validarProductoYMonto(
            Integer idServicio, Integer idProd, TipoPago tipoPago, BigDecimal monto) {
        List<GestoPagoProductoResponse> catalogo = gestoPagoProductService.listarProductosDisponibles(null);
        GestoPagoProductoResponse producto = catalogo.stream()
                .filter(p -> idServicio.equals(p.getIdServicio()) && idProd.equals(p.getIdProducto()))
                .findFirst()
                .orElseThrow(() -> new ProductoCatalogoException(
                        "El producto con idServicio=" + idServicio + " e idProd=" + idProd
                                + " no existe o no esta disponible en el catalogo de GestoPago",
                        true));

        Integer tipoFront = producto.getTipoFront();
        if (tipoPago == TipoPago.RECARGA && tipoFront != null && tipoFront == 2) {
            throw new ProductoCatalogoException(
                    "El producto '" + producto.getNombreProducto() + "' (tipoFront=2) corresponde a un pago de SERVICIO, no a una RECARGA",
                    false);
        }
        if (tipoPago == TipoPago.SERVICIO && tipoFront != null && tipoFront == 1) {
            throw new ProductoCatalogoException(
                    "El producto '" + producto.getNombreProducto() + "' (tipoFront=1) corresponde a una RECARGA, no a un pago de SERVICIO",
                    false);
        }

        BigDecimal precioCatalogo = parsearPrecio(producto.getPrecio());
        // Segun la documentacion de GestoPago:
        // En tipoFront=1 (RECARGA) un precio > 0 representa el precio fijo que debe cobrarse exactamente.
        // En tipoFront=2 (SERVICIO) el campo precio del catalogo representa la comision si el monto es variable,
        // pero si el producto es de precio fijo en RECARGA (o precio > 0 en tipoFront != 2), el monto debe coincidir exactamente.
        boolean esRecargaPrecioFijo = (tipoPago == TipoPago.RECARGA || (tipoFront != null && tipoFront == 1))
                && precioCatalogo.compareTo(BigDecimal.ZERO) > 0;

        if (esRecargaPrecioFijo && monto.compareTo(precioCatalogo) != 0) {
            throw new ProductoCatalogoException(
                    "El producto '" + producto.getNombreProducto() + "' tiene un precio fijo de "
                            + precioCatalogo.setScale(2, RoundingMode.HALF_UP)
                            + " y no acepta un monto distinto (" + monto.setScale(2, RoundingMode.HALF_UP) + ")",
                    false);
        }

        return producto;
    }

    private BigDecimal parsearPrecio(String precioTexto) {
        if (precioTexto == null || precioTexto.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(precioTexto.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String generarTraceUnico() {
        String trace;
        do {
            StringBuilder sb = new StringBuilder(LONGITUD_TRACE);
            for (int i = 0; i < LONGITUD_TRACE; i++) {
                sb.append(ALFABETO_TRACE.charAt(RANDOM.nextInt(ALFABETO_TRACE.length())));
            }
            trace = sb.toString();
        } while (pagoRepository.findByTrace(trace).isPresent());
        return trace;
    }

    private String generarCodigoSeisDigitos() {
        int valor = RANDOM.nextInt(1_000_000);
        return String.format("%06d", valor);
    }
}
