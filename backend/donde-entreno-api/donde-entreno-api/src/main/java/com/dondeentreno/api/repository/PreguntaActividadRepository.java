package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.PreguntaActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Preguntas y respuestas por actividad (script 29).
 */
public interface PreguntaActividadRepository
        extends JpaRepository<PreguntaActividad, Long> {

    List<PreguntaActividad> findByActividadIdAndEstadoOrderByCreatedAtDesc(
            Long actividadId,
            String estado
    );

    /** Rate limit: preguntas del usuario desde un instante (día). */
    long countByUsuarioIdAndCreatedAtGreaterThanEqual(
            Long usuarioId,
            OffsetDateTime desde
    );

    /**
     * Preguntas YA RESPONDIDAS de todas las actividades de un
     * publicador (Fase 5, tab del perfil). Solo respondidas: una
     * pregunta sin responder es mala señal en la vidriera del
     * publicador, y en el detalle de cada actividad se ven todas.
     *
     * PreguntaActividad guarda `actividadId` plano, así que el join
     * contra Actividad va explícito (igual que Valoracion).
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT p
              FROM PreguntaActividad p, Actividad a
             WHERE a.id = p.actividadId
               AND a.perfilPublicador.id = :perfilPublicadorId
               AND p.estado = :estado
               AND p.respuesta IS NOT NULL
             ORDER BY p.respondidaAt DESC
            """)
    List<PreguntaActividad> respondidasDePublicador(
            @org.springframework.data.repository.query.Param("perfilPublicadorId")
            Long perfilPublicadorId,
            @org.springframework.data.repository.query.Param("estado") String estado,
            org.springframework.data.domain.Pageable pageable
    );
}
