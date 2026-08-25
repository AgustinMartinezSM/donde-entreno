package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.MiembroActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Pertenencia a grupos de actividad (script 38).
 */
public interface MiembroActividadRepository extends JpaRepository<MiembroActividad, Long> {

    Optional<MiembroActividad> findByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);

    boolean existsByUsuarioIdAndActividadIdAndEstado(
            Long usuarioId,
            Long actividadId,
            String estado
    );

    long countByActividadIdAndEstado(Long actividadId, String estado);

    /** Los miembros activos, para el fan-out del aviso. */
    @Query("""
            SELECT m.usuarioId FROM MiembroActividad m
             WHERE m.actividadId = :actividadId
               AND m.estado = 'ACTIVO'
            """)
    List<Long> usuarioIdsActivosDe(@Param("actividadId") Long actividadId);

    /** Los grupos a los que pertenece alguien (su lista). */
    List<MiembroActividad> findByUsuarioIdAndEstadoOrderByUpdatedAtDesc(
            Long usuarioId,
            String estado
    );
}
