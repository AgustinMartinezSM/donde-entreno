package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.InteresActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Interés sobre actividades: quiero probar / ya probé (script 29).
 */
public interface InteresActividadRepository
        extends JpaRepository<InteresActividad, Long> {

    Optional<InteresActividad> findByUsuarioIdAndActividadId(
            Long usuarioId,
            Long actividadId
    );

    long deleteByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);

    /** Contador agregado y anónimo del detalle y las métricas. */
    long countByActividadIdAndEstado(Long actividadId, String estado);

    boolean existsByUsuarioIdAndActividadIdAndEstado(
            Long usuarioId,
            Long actividadId,
            String estado
    );

    /** Métricas del publicador: interés sobre TODAS sus actividades. */
    long countByActividadIdInAndEstado(
            java.util.Collection<Long> actividadIds,
            String estado
    );
}
