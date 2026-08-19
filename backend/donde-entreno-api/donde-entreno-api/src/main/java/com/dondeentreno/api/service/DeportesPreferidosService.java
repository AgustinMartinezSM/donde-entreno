package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.DeportePreferido;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.repository.DeportePreferidoRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deportes preferidos por cuenta (script 20): la fuente de verdad de
 * "Tus deportes".
 *
 * La UI edita el conjunto entero (checkboxes en /mi-cuenta), asi que el
 * contrato es reemplazo total: PUT con la lista completa de slugs.
 */
@Service
public class DeportesPreferidosService {

    private final DeportePreferidoRepository deportePreferidoRepository;
    private final DeporteRepository deporteRepository;

    public DeportesPreferidosService(
            DeportePreferidoRepository deportePreferidoRepository,
            DeporteRepository deporteRepository
    ) {
        this.deportePreferidoRepository = deportePreferidoRepository;
        this.deporteRepository = deporteRepository;
    }

    @Transactional(readOnly = true)
    public List<String> listar(Long usuarioId) {
        validarUserId(usuarioId);

        return deportePreferidoRepository.slugsDe(usuarioId);
    }

    /**
     * Reemplaza el conjunto completo. Los slugs que no matchean el
     * catalogo activo se ignoran en silencio (un deporte desactivado en
     * la lista vieja no puede hacer fallar el guardado de la nueva), y
     * los duplicados se colapsan conservando el primer orden.
     */
    @Transactional
    public List<String> reemplazar(Long usuarioId, List<String> slugs) {
        validarUserId(usuarioId);

        Map<String, Deporte> catalogoPorSlug = deporteRepository.findByActivoTrue()
                .stream()
                .collect(Collectors.toMap(Deporte::getSlug, Function.identity()));

        Set<String> slugsLimpios = new LinkedHashSet<>();

        for (String slug : slugs == null ? List.<String>of() : slugs) {
            if (slug == null) {
                continue;
            }

            String limpio = slug.trim();

            if (!limpio.isEmpty() && catalogoPorSlug.containsKey(limpio)) {
                slugsLimpios.add(limpio);
            }
        }

        deportePreferidoRepository.borrarDe(usuarioId);

        OffsetDateTime ahora = OffsetDateTime.now();
        List<DeportePreferido> filas = new ArrayList<>();
        /*
         * created_at escalonado en milisegundos: el listado ordena por
         * (createdAt, id) y el orden de eleccion es parte del contrato.
         */
        int indice = 0;

        for (String slug : slugsLimpios) {
            DeportePreferido fila = new DeportePreferido();
            fila.setUsuarioId(usuarioId);
            fila.setDeporteId(catalogoPorSlug.get(slug).getId());
            fila.setCreatedAt(ahora.plusNanos(indice * 1_000_000L));
            filas.add(fila);
            indice += 1;
        }

        deportePreferidoRepository.saveAll(filas);

        return List.copyOf(slugsLimpios);
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
