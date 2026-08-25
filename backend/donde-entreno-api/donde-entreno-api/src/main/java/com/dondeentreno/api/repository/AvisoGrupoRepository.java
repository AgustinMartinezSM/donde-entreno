package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.AvisoGrupo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

/**
 * Avisos del grupo (script 38).
 */
public interface AvisoGrupoRepository extends JpaRepository<AvisoGrupo, Long> {

    Page<AvisoGrupo> findByActividadIdAndEstadoOrderByCreatedAtDesc(
            Long actividadId,
            String estado,
            Pageable pageable
    );

    /**
     * Tope diario por actividad. Cuenta TODOS —incluso los borrados—
     * para que borrar y volver a avisar no saltee el limite (misma
     * leccion que el script 34).
     */
    long countByActividadIdAndCreatedAtGreaterThanEqual(
            Long actividadId,
            OffsetDateTime desde
    );
}
