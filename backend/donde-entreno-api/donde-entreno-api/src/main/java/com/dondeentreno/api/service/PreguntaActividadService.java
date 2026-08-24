package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PreguntaActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PreguntaActividad;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.PreguntaActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Preguntas y respuestas por actividad (script 29, patrón
 * MercadoLibre). Publica directo (moderación por reportes); una
 * respuesta única del publicador dueño; borrar la propia solo si no
 * fue respondida; tope diario contra la base.
 */
@Service
public class PreguntaActividadService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String ESTADO_VISIBLE = "VISIBLE";
    private static final String ESTADO_OCULTA = "OCULTA_POR_ADMIN";
    private static final String ESTADO_ELIMINADA = "ELIMINADA_POR_USUARIO";
    private static final int MAX_PREGUNTAS_POR_DIA = 5;
    private static final int MAX_PREGUNTA = 500;
    private static final int MAX_RESPUESTA = 1000;

    private final PreguntaActividadRepository preguntaActividadRepository;
    private final ActividadRepository actividadRepository;
    private final NotificacionService notificacionService;

    public PreguntaActividadService(
            PreguntaActividadRepository preguntaActividadRepository,
            ActividadRepository actividadRepository,
            NotificacionService notificacionService
    ) {
        this.preguntaActividadRepository = preguntaActividadRepository;
        this.actividadRepository = actividadRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public PreguntaActividadDTO preguntar(Long usuarioId, Long actividadId, String pregunta) {
        validarUserId(usuarioId);

        String preguntaLimpia = pregunta != null ? pregunta.trim() : "";
        if (preguntaLimpia.isEmpty()) {
            throw new FiltroInvalidoException("La pregunta no puede estar vacia.");
        }
        if (preguntaLimpia.length() > MAX_PREGUNTA) {
            preguntaLimpia = preguntaLimpia.substring(0, MAX_PREGUNTA);
        }

        Actividad actividad = buscarActividadPublica(actividadId);

        /* Tope diario contra la base (día argentino, patrón check-in). */
        OffsetDateTime inicioDeHoy = LocalDate.now(ZONA_ARGENTINA)
                .atStartOfDay(ZONA_ARGENTINA)
                .toOffsetDateTime();
        long preguntasHoy = preguntaActividadRepository
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(usuarioId, inicioDeHoy);
        if (preguntasHoy >= MAX_PREGUNTAS_POR_DIA) {
            throw new FiltroInvalidoException(
                    "Llegaste al tope de preguntas por hoy. Probá de nuevo mañana."
            );
        }

        PreguntaActividad nueva = new PreguntaActividad();
        nueva.setActividadId(actividadId);
        nueva.setUsuarioId(usuarioId);
        nueva.setPregunta(preguntaLimpia);
        nueva.setEstado(ESTADO_VISIBLE);
        nueva.setCreatedAt(OffsetDateTime.now());

        PreguntaActividad guardada = preguntaActividadRepository.save(nueva);

        if (actividad.getPerfilPublicador() != null
                && actividad.getPerfilPublicador().getUsuario() != null) {
            notificacionService.emitir(
                    actividad.getPerfilPublicador().getUsuario().getId(),
                    "PREGUNTA_NUEVA",
                    "Nueva pregunta en \"" + actividad.getTitulo() + "\": "
                            + recortarParaTitulo(preguntaLimpia),
                    "/actividades/" + actividad.getSlug() + "#preguntas"
            );
        }

        return toDTO(guardada, usuarioId);
    }

    /** Responde el DUEÑO del perfil de la actividad; única respuesta. */
    @Transactional
    public PreguntaActividadDTO responder(Long usuarioId, Long preguntaId, String respuesta) {
        validarUserId(usuarioId);

        String respuestaLimpia = respuesta != null ? respuesta.trim() : "";
        if (respuestaLimpia.isEmpty()) {
            throw new FiltroInvalidoException("La respuesta no puede estar vacia.");
        }
        if (respuestaLimpia.length() > MAX_RESPUESTA) {
            respuestaLimpia = respuestaLimpia.substring(0, MAX_RESPUESTA);
        }

        PreguntaActividad pregunta = preguntaActividadRepository.findById(preguntaId)
                .filter(encontrada -> ESTADO_VISIBLE.equals(encontrada.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la pregunta."));

        Actividad actividad = actividadRepository.findById(pregunta.getActividadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la actividad."));

        boolean esDuenio = actividad.getPerfilPublicador() != null
                && actividad.getPerfilPublicador().getUsuario() != null
                && actividad.getPerfilPublicador().getUsuario().getId().equals(usuarioId);
        if (!esDuenio) {
            /* 404, no 403: no se delata la existencia (patrón likes). */
            throw new RecursoNoEncontradoException("No se encontro la pregunta.");
        }

        pregunta.setRespuesta(respuestaLimpia);
        pregunta.setRespondidaAt(OffsetDateTime.now());

        PreguntaActividad guardada = preguntaActividadRepository.save(pregunta);

        notificacionService.emitir(
                pregunta.getUsuarioId(),
                "RESPUESTA_PREGUNTA",
                "Respondieron tu pregunta en \"" + actividad.getTitulo() + "\".",
                "/actividades/" + actividad.getSlug() + "#preguntas"
        );

        return toDTO(guardada, usuarioId);
    }

    /** Borrar la propia SOLO si no fue respondida (regla del plan). */
    @Transactional
    public void eliminarPropia(Long usuarioId, Long preguntaId) {
        validarUserId(usuarioId);

        PreguntaActividad pregunta = preguntaActividadRepository.findById(preguntaId)
                .filter(encontrada -> encontrada.getUsuarioId().equals(usuarioId))
                .filter(encontrada -> ESTADO_VISIBLE.equals(encontrada.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la pregunta."));

        if (pregunta.getRespuesta() != null) {
            throw new FiltroInvalidoException(
                    "Una pregunta respondida ya ayuda a otras personas y no se puede borrar."
            );
        }

        pregunta.setEstado(ESTADO_ELIMINADA);
        preguntaActividadRepository.save(pregunta);
    }

    @Transactional
    public void ocultarPorAdmin(Long preguntaId) {
        PreguntaActividad pregunta = preguntaActividadRepository.findById(preguntaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la pregunta."));

        pregunta.setEstado(ESTADO_OCULTA);
        preguntaActividadRepository.save(pregunta);
    }

    /** Listado público. usuarioId puede ser null (visitante). */
    @Transactional(readOnly = true)
    public List<PreguntaActividadDTO> listarDe(Long actividadId, Long usuarioId) {
        return preguntaActividadRepository
                .findByActividadIdAndEstadoOrderByCreatedAtDesc(actividadId, ESTADO_VISIBLE)
                .stream()
                .map(pregunta -> toDTO(pregunta, usuarioId))
                .toList();
    }

    /**
     * Preguntas RESPONDIDAS del publicador (Fase 5, tab del perfil).
     * Cada una con su actividad, porque acá se mezclan varias.
     */
    @Transactional(readOnly = true)
    public List<PreguntaActividadDTO> listarRespondidasDePublicador(
            Long perfilPublicadorId,
            Long usuarioId,
            int limite
    ) {
        List<PreguntaActividad> preguntas = preguntaActividadRepository
                .respondidasDePublicador(
                        perfilPublicadorId,
                        ESTADO_VISIBLE,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                Math.min(Math.max(limite, 1), 50)
                        )
                );

        List<Long> actividadIds = preguntas.stream()
                .map(PreguntaActividad::getActividadId)
                .distinct()
                .toList();
        java.util.Map<Long, Actividad> actividades =
                actividadRepository.findAllById(actividadIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Actividad::getId,
                                actividad -> actividad
                        ));

        return preguntas.stream()
                .map(pregunta -> {
                    PreguntaActividadDTO dto = toDTO(pregunta, usuarioId);
                    Actividad actividad = actividades.get(pregunta.getActividadId());

                    if (actividad != null) {
                        dto.setActividadTitulo(actividad.getTitulo());
                        dto.setActividadSlug(actividad.getSlug());
                    }

                    return dto;
                })
                .toList();
    }

    /** ¿Visible? (para reportarla). */
    @Transactional(readOnly = true)
    public boolean esVisible(Long preguntaId) {
        return preguntaActividadRepository.findById(preguntaId)
                .filter(pregunta -> ESTADO_VISIBLE.equals(pregunta.getEstado()))
                .isPresent();
    }

    private Actividad buscarActividadPublica(Long actividadId) {
        return actividadRepository.findById(actividadId)
                .filter(actividad -> Boolean.TRUE.equals(actividad.getActiva())
                        && "PUBLICADA".equals(actividad.getEstadoPublicacion())
                        && actividad.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la actividad."));
    }

    private PreguntaActividadDTO toDTO(PreguntaActividad pregunta, Long usuarioActualId) {
        return new PreguntaActividadDTO(
                pregunta.getId(),
                pregunta.getPregunta(),
                pregunta.getRespuesta(),
                pregunta.getRespondidaAt(),
                pregunta.getUsuarioId().equals(usuarioActualId),
                pregunta.getCreatedAt()
        );
    }

    private String recortarParaTitulo(String pregunta) {
        return pregunta.length() <= 60 ? pregunta : pregunta.substring(0, 60) + "...";
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
