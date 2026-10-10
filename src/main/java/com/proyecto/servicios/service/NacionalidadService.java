package com.proyecto.servicios.service;

import com.proyecto.servicios.client.RestCountriesClient;
import com.proyecto.servicios.model.CatalogoItem;
import com.proyecto.servicios.model.RestCountriesListResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo de nacionalidades (paises). Fuente principal: API REST Countries v5.
 * Respaldo: lista ISO de Java en espanol. El resultado se guarda en memoria.
 */
@Slf4j
@Service
public class NacionalidadService {

    private static final String CAMPOS = "names.common,names.translations,codes.alpha_2";
    private static final int TAMANO_PAGINA = 100;
    private static final int MAX_PAGINAS = 5;
    private static final int MINIMO_PAISES_ESPERADOS = 150;
    private static final Duration REINTENTO_TRAS_FALLO = Duration.ofMinutes(10);
    private static final Locale ESPANOL = Locale.forLanguageTag("es-MX");

    private final PaisesIsoProvider paisesIsoProvider;
    private final RestCountriesClient restCountriesClient;
    private final String apiKey;
    private final Duration vigencia;

    private volatile CatalogoEnMemoria catalogo;

    public NacionalidadService(PaisesIsoProvider paisesIsoProvider,
                               RestCountriesClient restCountriesClient,
                               @Value("${restcountries.api-key:}") String apiKey,
                               @Value("${restcountries.cache-hours:24}") long horasVigencia) {
        this.paisesIsoProvider = paisesIsoProvider;
        this.restCountriesClient = restCountriesClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.vigencia = Duration.ofHours(Math.max(1, horasVigencia));
    }

    /** Todas las nacionalidades, ordenadas alfabeticamente por nombre en espanol. */
    public List<CatalogoItem> listar() {
        return obtenerCatalogo().entrySet().stream()
                .map(e -> new CatalogoItem(e.getKey(), e.getValue()))
                .toList();
    }

    /** true si el codigo de 2 letras (MX, US...) existe en el catalogo. */
    public boolean esCodigoValido(String codigo) {
        return codigo != null
                && obtenerCatalogo().containsKey(codigo.trim().toUpperCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ cache

    private Map<String, String> obtenerCatalogo() {
        CatalogoEnMemoria actual = catalogo;
        if (estaVigente(actual)) {
            return actual.paises();
        }
        synchronized (this) {
            actual = catalogo;
            if (estaVigente(actual)) {
                return actual.paises();
            }
            catalogo = reconstruir(actual);
            return catalogo.paises();
        }
    }

    private boolean estaVigente(CatalogoEnMemoria actual) {
        return actual != null && Instant.now().isBefore(actual.expiraEn());
    }

    private CatalogoEnMemoria reconstruir(CatalogoEnMemoria anterior) {
        if (apiKey.isBlank()) {
            log.info("Sin RESTCOUNTRIES_API_KEY: el catalogo de nacionalidades usa la lista de respaldo");
            return new CatalogoEnMemoria(desdeRespaldo(), Instant.now().plus(vigencia));
        }
        Map<String, String> desdeApi = cargarDesdeApi();
        if (desdeApi != null) {
            return new CatalogoEnMemoria(desdeApi, Instant.now().plus(vigencia));
        }
        Map<String, String> base = anterior != null ? anterior.paises() : desdeRespaldo();
        return new CatalogoEnMemoria(base, Instant.now().plus(REINTENTO_TRAS_FALLO));
    }

    // -------------------------------------------------------------------- API

    /** Devuelve el catalogo completo desde la API, o null si algo fallo (sin lanzar excepcion). */
    private Map<String, String> cargarDesdeApi() {
        try {
            List<CatalogoItem> items = new ArrayList<>();
            int offset = 0;
            for (int pagina = 0; pagina < MAX_PAGINAS; pagina++) {
                RestCountriesListResponse respuesta = restCountriesClient
                        .listarPaises("Bearer " + apiKey, TAMANO_PAGINA, offset, CAMPOS);
                if (respuesta == null || respuesta.data() == null || respuesta.data().objects() == null) {
                    log.warn("La API de paises devolvio una respuesta sin datos; se usa la lista de respaldo");
                    return null;
                }
                respuesta.data().objects().stream()
                        .map(p -> new CatalogoItem(p.codigo(), p.nombreEnEspanol()))
                        .filter(this::esItemCompleto)
                        .forEach(items::add);
                if (!respuesta.data().hayMas()) {
                    break;
                }
                offset += TAMANO_PAGINA;
            }
            if (items.size() < MINIMO_PAISES_ESPERADOS) {
                log.warn("La API de paises devolvio solo {} paises (se esperaban mas de {}); se usa la lista de respaldo",
                        items.size(), MINIMO_PAISES_ESPERADOS);
                return null;
            }
            Map<String, String> ordenado = ordenar(items);
            log.info("Catalogo de nacionalidades cargado desde la API ({} paises)", ordenado.size());
            return ordenado;
        } catch (FeignException e) {
            log.warn("La API de paises no esta disponible (estado {}, {}); se usa la lista de respaldo",
                    e.status(), e.getClass().getSimpleName());
            return null;
        } catch (RuntimeException e) {
            log.warn("Error al consultar la API de paises ({}); se usa la lista de respaldo",
                    e.getClass().getSimpleName());
            return null;
        }
    }

    private boolean esItemCompleto(CatalogoItem item) {
        return item.codigo() != null && !item.codigo().isBlank()
                && item.etiqueta() != null && !item.etiqueta().isBlank();
    }

    private Map<String, String> ordenar(List<CatalogoItem> items) {
        Collator collator = Collator.getInstance(ESPANOL);
        Map<String, String> mapa = new LinkedHashMap<>();
        items.stream()
                .sorted(Comparator.comparing(CatalogoItem::etiqueta, collator))
                .forEach(i -> mapa.putIfAbsent(i.codigo().trim().toUpperCase(Locale.ROOT), i.etiqueta()));
        return Collections.unmodifiableMap(mapa);
    }

    // ---------------------------------------------------------------- respaldo

    private Map<String, String> desdeRespaldo() {
        Map<String, String> mapa = new LinkedHashMap<>();
        paisesIsoProvider.listar().forEach(item -> mapa.put(item.codigo(), item.etiqueta()));
        return Collections.unmodifiableMap(mapa);
    }

    private record CatalogoEnMemoria(Map<String, String> paises, Instant expiraEn) {
    }
}