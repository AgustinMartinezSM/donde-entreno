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
}
