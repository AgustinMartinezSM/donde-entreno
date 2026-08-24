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

    /**
     * Ranking global de actividades por tipo de interacción en una
     * ventana (Fase 6): las más vistas de los últimos N días.
     * Filas: [actividadId, cantidad], de mayor a menor.
     *
     * El `actividadId IS NOT NULL` NO es decorativo: desde el script
     * 31 la columna es nullable porque hay eventos que cuelgan del
     * PERFIL. Sin este filtro, los clicks del perfil se cuelan en el
     * conteo por actividad — un bug que no falla, solo miente.
     *
     * El índice (actividad_id, tipo, created_at) cubre el query.
     */
    @Query("""
            SELECT e.actividadId, COUNT(e)
              FROM EventoInteraccion e
             WHERE e.tipo = :tipo
               AND e.actividadId IS NOT NULL
               AND e.createdAt >= :desde
             GROUP BY e.actividadId
             ORDER BY COUNT(e) DESC
            """)
    List<Object[]> rankingDeActividades(
            @Param("tipo") String tipo,
            @Param("desde") OffsetDateTime desde,
            org.springframework.data.domain.Pageable pageable
    );
}
