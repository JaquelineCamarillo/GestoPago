package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CatalogoItem;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo de nacionalidades (paises). Se construye una sola vez y se guarda en memoria.
 * Fuente actual: lista ISO de Java en espanol. La API externa de paises se agregara
 * como fuente principal, dejando esta lista como respaldo.
 */
@Service
public class NacionalidadService {

    private final PaisesIsoProvider paisesIsoProvider;
    private volatile Map<String, String> catalogo;

    public NacionalidadService(PaisesIsoProvider paisesIsoProvider) {
        this.paisesIsoProvider = paisesIsoProvider;
    }

    /** Todas las nacionalidades, ordenadas alfabeticamente por nombre. */
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

    private Map<String, String> obtenerCatalogo() {
        Map<String, String> local = catalogo;
        if (local == null) {
            synchronized (this) {
                local = catalogo;
                if (local == null) {
                    local = construirCatalogo();
                    catalogo = local;
                }
            }
        }
        return local;
    }

    private Map<String, String> construirCatalogo() {
        Map<String, String> mapa = new LinkedHashMap<>();
        paisesIsoProvider.listar().forEach(item -> mapa.put(item.codigo(), item.etiqueta()));
        return Collections.unmodifiableMap(mapa);
    }
}