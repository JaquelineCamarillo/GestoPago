package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CatalogoItem;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lista de paises en espanol construida con los datos ISO 3166 que ya trae Java.
 * Funciona sin internet; es el respaldo cuando la API externa de paises no responde.
 */
@Component
public class PaisesIsoProvider {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-MX");

    public List<CatalogoItem> listar() {
        Collator collator = Collator.getInstance(ESPANOL);
        return Arrays.stream(Locale.getISOCountries())
                .map(codigo -> new CatalogoItem(codigo, nombreEnEspanol(codigo)))
                .filter(item -> !item.etiqueta().isBlank()
                        && !item.etiqueta().equalsIgnoreCase(item.codigo()))
                .sorted(Comparator.comparing(CatalogoItem::etiqueta, collator))
                .toList();
    }

    private String nombreEnEspanol(String codigoIso) {
        return new Locale.Builder().setRegion(codigoIso).build().getDisplayCountry(ESPANOL);
    }
}