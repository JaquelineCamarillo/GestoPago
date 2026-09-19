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
import org.springframework.data.redis.core.RedisTemplate;
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
    private static final String REDIS_KEY_PRODUCTOS = "gestopago:productos";
    private static final int TIPO_FRONT_POR_DEFECTO = 0;

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public GestoPagoProductServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                                       GestoPagoTokenService gestoPagoTokenService,
                                       GestoPagoProductoRepository productoRepository,
                                       GestoPagoProductMapper productMapper,
                                       RedisTemplate<String, Object> redisTemplate) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.productoRepository = productoRepository;
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Scheduled(cron = "${gestopago.productos.sync-cron:0 0 6 * * *}")
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

            List<GestoPagoProductoXml> productosXml = response.getProductos() == null
                    ? List.of() : response.getProductos();

            int total;
            if (guardarEnRedis(productosXml)) {
                total = productosXml.size();
            } else {
                total = guardarEnBaseDeDatos(productosXml);
            }

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
    public List<GestoPagoProductoResponse> listarProductosDisponibles(Integer tipoFront) {
        List<GestoPagoProductoResponse> catalogo = leerDesdeRedis();

        if (catalogo != null) {
            log.info("Catalogo leido desde Redis ({} productos)", catalogo.size());
        } else {
            log.warn("Redis no disponible para lectura, usando base de datos local como respaldo");
            catalogo = productoRepository.findByActivoTrue().stream()
                    .map(this::aResponse)
                    .collect(Collectors.toList());
        }

        if (tipoFront == null) {
            return catalogo;
        }
        return catalogo.stream()
                .filter(p -> tipoFront.equals(p.getTipoFront()))
                .collect(Collectors.toList());
    }

    private boolean guardarEnRedis(List<GestoPagoProductoXml> productosXml) {
        try {
            List<GestoPagoProductoResponse> productos = productosXml.stream()
                    .map(this::aResponse)
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(REDIS_KEY_PRODUCTOS, productos);
            log.info("Catalogo guardado en Redis ({} productos)", productos.size());
            return true;
        } catch (Exception e) {
            log.warn("Redis no disponible al guardar el catalogo, se usara la base de datos local: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<GestoPagoProductoResponse> leerDesdeRedis() {
        try {
            Object cacheado = redisTemplate.opsForValue().get(REDIS_KEY_PRODUCTOS);
            return (cacheado instanceof List<?>) ? (List<GestoPagoProductoResponse>) cacheado : null;
        } catch (Exception e) {
            log.warn("Redis no disponible al leer el catalogo: {}", e.getMessage());
            return null;
        }
    }

    private int guardarEnBaseDeDatos(List<GestoPagoProductoXml> productosXml) {
        List<GestoPagoProducto> guardados = productosXml.stream()
                .map(this::guardarOActualizar)
                .collect(Collectors.toList());
        log.info("Catalogo guardado en base de datos local ({} productos)", guardados.size());
        return guardados.size();
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
        if (entidad.getTipoFront() == null) {
            entidad.setTipoFront(TIPO_FRONT_POR_DEFECTO);
        }
        return productoRepository.save(entidad);
    }

    private GestoPagoProductoResponse aResponse(GestoPagoProductoXml xml) {
        Integer tipoFront = xml.getTipoFront() != null ? xml.getTipoFront() : TIPO_FRONT_POR_DEFECTO;
        return new GestoPagoProductoResponse(
                xml.getIdProducto(), xml.getIdServicio(), xml.getServicio(), xml.getProducto(),
                xml.getPrecio(), tipoFront);
    }

    private GestoPagoProductoResponse aResponse(GestoPagoProducto entidad) {
        Integer tipoFront = entidad.getTipoFront() != null ? entidad.getTipoFront() : TIPO_FRONT_POR_DEFECTO;
        return new GestoPagoProductoResponse(
                entidad.getIdProducto(), entidad.getIdServicio(), entidad.getNombreServicio(),
                entidad.getNombreProducto(), entidad.getPrecio(), tipoFront);
    }

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