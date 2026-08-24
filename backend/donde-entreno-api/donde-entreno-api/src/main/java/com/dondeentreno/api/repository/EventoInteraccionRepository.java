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

    /**
     * Conteos por tipo de las interacciones del PERFIL (Fase 5): las
     * que no cuelgan de ninguna actividad. Filas: [tipo, cantidad].
     */
    @Query("""
            SELECT e.tipo, COUNT(e)
              FROM EventoInteraccion e
             WHERE e.perfilPublicadorId = :perfilPublicadorId
               AND e.createdAt >= :desde
             GROUP BY e.tipo
            """)
    List<Object[]> contarPorPerfilYTipo(
            @Param("perfilPublicadorId") Long perfilPublicadorId,
            @Param("desde") OffsetDateTime desde
    );
}
