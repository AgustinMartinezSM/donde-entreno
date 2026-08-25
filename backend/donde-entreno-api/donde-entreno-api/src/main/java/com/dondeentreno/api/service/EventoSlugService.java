package com.dondeentreno.api.service;

import com.dondeentreno.api.repository.EventoDeportivoRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Slug único del evento (Fase 9). Mismo molde que
 * `PerfilPublicadorSlugService`: el título normalizado y, si ya
 * existe, un sufijo numérico.
 *
 * Dos torneos con el mismo nombre en años distintos son el caso
 * normal, no la excepción, así que el sufijo va a usarse seguido.
 */
@Service
public class EventoSlugService {

    private static final int SLUG_MAX_LENGTH = 180;
    private static final String BASE_FALLBACK = "evento";

    private final EventoDeportivoRepository eventoDeportivoRepository;

    public EventoSlugService(EventoDeportivoRepository eventoDeportivoRepository) {
        this.eventoDeportivoRepository = eventoDeportivoRepository;
    }

    public String generarSlugUnico(String titulo) {
        String base = generarBaseSlug(titulo);
        String slug = limitarSlug(base, "");
        int contador = 2;

        while (eventoDeportivoRepository.existsBySlug(slug)) {
            String sufijo = "-" + contador;
            slug = limitarSlug(base, sufijo) + sufijo;
            contador++;
        }

        return slug;
    }

    private String generarBaseSlug(String texto) {
        if (texto == null) {
            return BASE_FALLBACK;
        }

        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (normalizado.isBlank()) {
            return BASE_FALLBACK;
        }

        return normalizado;
    }

    private String limitarSlug(String base, String sufijo) {
        int longitudMaxima = SLUG_MAX_LENGTH - sufijo.length();
        String slug = base.length() <= longitudMaxima
                ? base
                : base.substring(0, longitudMaxima);

        slug = slug.replaceAll("-+$", "");
        if (slug.isBlank()) {
            return BASE_FALLBACK;
        }

        return slug;
    }
}
