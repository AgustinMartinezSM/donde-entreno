package com.dondeentreno.api.service;

import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Generación del slug de perfiles de publicador (script 27), con el
 * mismo normalizador que los slugs de actividades: NFD sin acentos,
 * minúsculas, guiones, y sufijo -2, -3... ante colisión.
 *
 * El slug es ESTABLE: se genera UNA vez al crear el perfil y renombrar
 * no lo cambia — los links compartidos no se rompen.
 */
@Service
public class PerfilPublicadorSlugService {

    private static final int SLUG_MAX_LENGTH = 150;
    private static final String BASE_FALLBACK = "publicador";

    private final PerfilPublicadorRepository perfilPublicadorRepository;

    public PerfilPublicadorSlugService(PerfilPublicadorRepository perfilPublicadorRepository) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
    }

    public String generarSlugUnico(String nombre) {
        String base = generarBaseSlug(nombre);
        String slug = limitarSlug(base, "");
        int contador = 2;

        while (perfilPublicadorRepository.existsBySlug(slug)) {
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
