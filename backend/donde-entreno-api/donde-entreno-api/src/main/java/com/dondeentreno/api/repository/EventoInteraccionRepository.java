package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.EventoInteraccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Interacciones anónimas (script 28, Fase 2 social).
 */
public interface EventoInteraccionRepository
        extends JpaRepository<EventoInteraccion, Long> {

    /**
     * Conteos por actividad y tipo desde un instante, agrupados en UN
     * query (patrón del contador de seguidores): las métricas del
     * publicador piden N actividades sin N×M counts.
     */
    @Query("""
            SELECT e.actividadId, e.tipo, COUNT(e)
              FROM EventoInteraccion e
             WHERE e.actividadId IN :actividadIds
               AND e.createdAt >= :desde
             GROUP BY e.actividadId, e.tipo
            """)
    List<Object[]> contarPorActividadYTipo(
            @Param("actividadIds") Collection<Long> actividadIds,
            @Param("desde") OffsetDateTime desde
    );
}
