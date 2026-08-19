package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.FavoritoActividad;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.ActividadMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Favoritos por cuenta (script 20): la fuente de verdad de "Guardados".
 *
 * La identidad publica de la actividad es el SLUG (igual que en todo el
 * frontend): guardar y quitar van por slug, y el listado devuelve el
 * mismo ActividadDTO de las cards publicas — snapshot vivo desde la
 * base, no el congelado del dia en que se guardo.
 */
@Service
public class FavoritosUsuarioService {

    private static final String ESTADO_ACTIVIDAD_PUBLICADA = "PUBLICADA";

    private final FavoritoActividadRepository favoritoActividadRepository;
    private final ActividadRepository actividadRepository;
    private final ImagenService imagenService;

    public FavoritosUsuarioService(
            FavoritoActividadRepository favoritoActividadRepository,
            ActividadRepository actividadRepository,
            ImagenService imagenService
    ) {
        this.favoritoActividadRepository = favoritoActividadRepository;
        this.actividadRepository = actividadRepository;
        this.imagenService = imagenService;
    }

    /**
     * Los favoritos del usuario como cards publicas, mas recientes
     * primero. Dos pasos como el feed: ids del favorito, actividades con
     * su propio query filtrado — una despublicada desaparece de la lista
     * sin romperla (la fila queda por si se republica).
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listar(Long usuarioId) {
        validarUserId(usuarioId);

        List<FavoritoActividad> favoritos =
                favoritoActividadRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);

        if (favoritos.isEmpty()) {
            return List.of();
        }

        List<Long> idsOrdenados = favoritos.stream()
                .map(FavoritoActividad::getActividadId)
                .toList();

        Map<Long, Actividad> actividadesPorId = actividadRepository
                .findByIdInAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        idsOrdenados,
                        ESTADO_ACTIVIDAD_PUBLICADA
                )
                .stream()
                .collect(Collectors.toMap(Actividad::getId, Function.identity()));

        /* El orden lo manda el favorito (guardado mas reciente primero). */
        List<ActividadDTO> resultado = idsOrdenados.stream()
                .map(actividadesPorId::get)
                .filter(actividad -> actividad != null)
                .map(ActividadMapper::toDTO)
                .toList();

        imagenService.asignarImagenPrincipal(resultado);

        return resultado;
    }

    /**
     * Guarda por slug. Idempotente: repetir no duplica (UNIQUE en la
     * tabla; la carrera de dos requests simultaneos cae en el catch).
     * Slug inexistente o no publicado: 404 — no se puede guardar lo que
     * no se puede ver.
     */
    @Transactional
    public void guardar(Long usuarioId, String slug) {
        validarUserId(usuarioId);

        Actividad actividad = buscarActividadPublicada(slug);

        if (favoritoActividadRepository.existsByUsuarioIdAndActividadId(usuarioId, actividad.getId())) {
            return;
        }

        FavoritoActividad favorito = new FavoritoActividad();
        favorito.setUsuarioId(usuarioId);
        favorito.setActividadId(actividad.getId());
        favorito.setCreatedAt(OffsetDateTime.now());

        try {
            favoritoActividadRepository.saveAndFlush(favorito);
        } catch (DataIntegrityViolationException excepcion) {
            /* Otro request lo guardo en el medio: mismo resultado, nada que hacer. */
        }
    }

    /**
     * Quita por slug. Idempotente y sin 404: quitar lo que no esta (o lo
     * que ya no existe) da lo mismo que quitarlo — 204 siempre.
     */
    @Transactional
    public void quitar(Long usuarioId, String slug) {
        validarUserId(usuarioId);

        actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion(
                slugNormalizado(slug),
                ESTADO_ACTIVIDAD_PUBLICADA
        ).ifPresent(actividad ->
                favoritoActividadRepository.deleteByUsuarioIdAndActividadId(usuarioId, actividad.getId())
        );
    }

    private Actividad buscarActividadPublicada(String slug) {
        return actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion(
                slugNormalizado(slug),
                ESTADO_ACTIVIDAD_PUBLICADA
        ).orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada."));
    }

    private String slugNormalizado(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new RecursoNoEncontradoException("Actividad no encontrada.");
        }

        return slug.trim();
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
