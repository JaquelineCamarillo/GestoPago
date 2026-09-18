package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoBadResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoIntegrationException;
import com.proyecto.servicios.mapper.GestoPagoProductMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CODIGO_EXITO = "01";

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoProductMapper productMapper;

    public GestoPagoProductServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                                       GestoPagoTokenService gestoPagoTokenService,
                                       GestoPagoProductoRepository productoRepository,
                                       GestoPagoProductMapper productMapper) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.productoRepository = productoRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Scheduled(cron = "${gestopago.productos.sync-cron:0 0 3 * * *}")
    @Transactional
    public void sincronizarCatalogo() {
        log.info("INICIO sincronizacion de catalogo GestoPago (getProductList.do)");
        try {
            GestoPagoProductListXmlResponse response = invocarConReintentoPorTokenExpirado();

            if (response == null || response.getMensaje() == null
                    || !CODIGO_EXITO.equals(response.getMensaje().getCodigo())) {
                String texto = (response != null && response.getMensaje() != null)
                        ? response.getMensaje().getTexto() : "Respuesta vacia de GestoPago";
                String codigo = (response != null && response.getMensaje() != null)
                        ? response.getMensaje().getCodigo() : null;
                throw new GestoPagoBadResponseException(texto, codigo);
            }

            int total = response.getProductos() == null ? 0
                    : (int) response.getProductos().stream().map(this::guardarOActualizar).count();

            log.info("FIN sincronizacion de catalogo GestoPago - productos sincronizados={}", total);
        } catch (GestoPagoIntegrationException e) {
            log.error("FIN sincronizacion de catalogo GestoPago con error controlado: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("FIN sincronizacion de catalogo GestoPago con error NO controlado", e);
            throw new GestoPagoCommunicationException("Error inesperado al sincronizar catalogo GestoPago", e);
        }
    }

    @Override
    public List<GestoPagoProductoResponse> listarProductosDisponibles() {
        return productoRepository.findByActivoTrue().stream()
                .map(p -> new GestoPagoProductoResponse(
                        p.getIdProducto(), p.getIdServicio(), p.getNombreServicio(),
                        p.getNombreProducto(), p.getPrecio()))
                .collect(Collectors.toList());
    }

    private GestoPagoProducto guardarOActualizar(GestoPagoProductoXml xml) {
        GestoPagoProducto entidad = productoRepository
                .findByIdProductoAndIdServicio(xml.getIdProducto(), xml.getIdServicio())
                .map(existing -> {
                    productMapper.updateEntity(xml, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    GestoPagoProducto nuevo = productMapper.toEntity(xml);
                    nuevo.setActivo(true);
                    return nuevo;
                });
        return productoRepository.save(entidad);
    }

    /**
     * Invoca getProductList.do. Si el token expira justo al usarlo, se fuerza UNA
     * renovacion y se reintenta una sola vez (evita loops infinitos ante un problema real
     * de credenciales).
     */
    private GestoPagoProductListXmlResponse invocarConReintentoPorTokenExpirado() {
        String token = gestoPagoTokenService.obtenerTokenBearer();
        try {
            return gestoPagoProductClient.obtenerListaProductos(BEARER_PREFIX + token);
        } catch (GestoPagoAuthenticationException e) {
            if (e.getMotivo() == GestoPagoAuthenticationException.Motivo.TOKEN_EXPIRADO) {
                log.warn("Token GestoPago expirado durante la llamada, renovando y reintentando una vez");
                gestoPagoTokenService.renovarToken();
                String nuevoToken = gestoPagoTokenService.obtenerTokenBearer();
                return gestoPagoProductClient.obtenerListaProductos(BEARER_PREFIX + nuevoToken);
            }
            throw e;
        }
    }
}