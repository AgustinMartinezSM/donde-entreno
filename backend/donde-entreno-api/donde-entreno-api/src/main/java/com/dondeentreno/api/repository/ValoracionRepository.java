package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Valoracion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Valoraciones 1-5 (script 29).
 */
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    Optional<Valoracion> findByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);

    long deleteByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);

    Page<Valoracion> findByActividadIdAndEstadoOrderByCreatedAtDesc(
            Long actividadId,
            String estado,
            Pageable pageable
    );

    long countByActividadIdAndEstado(Long actividadId, String estado);

    /**
     * Distribución 1..5 de las visibles en UN query agrupado.
     * Filas: [puntaje, cantidad].
     */
    @Query("""
            SELECT v.puntaje, COUNT(v)
              FROM Valoracion v
             WHERE v.actividadId = :actividadId
               AND v.estado = 'VISIBLE'
             GROUP BY v.puntaje
            """)
    List<Object[]> distribucionVisibles(@Param("actividadId") Long actividadId);

    /**
     * Distribución 1..5 de TODAS las actividades de un publicador
     * (Fase 5). Valoracion guarda `actividadId` plano, sin relación
     * JPA, así que el join contra Actividad va explícito.
     * Filas: [puntaje, cantidad].
     */
    @Query("""
            SELECT v.puntaje, COUNT(v)
              FROM Valoracion v, Actividad a
             WHERE a.id = v.actividadId
               AND a.perfilPublicador.id = :perfilPublicadorId
               AND v.estado = 'VISIBLE'
             GROUP BY v.puntaje
            """)
    List<Object[]> distribucionVisiblesDePublicador(
            @Param("perfilPublicadorId") Long perfilPublicadorId
    );

    /**
     * Distribución por PUBLICADOR para varios perfiles de una (los
     * listados): evita el N+1 del batch de perfiles.
     * Filas: [perfilPublicadorId, puntaje, cantidad].
     */
    @Query("""
            SELECT a.perfilPublicador.id, v.puntaje, COUNT(v)
              FROM Valoracion v, Actividad a
             WHERE a.id = v.actividadId
               AND a.perfilPublicador.id IN :perfilPublicadorIds
               AND v.estado = 'VISIBLE'
             GROUP BY a.perfilPublicador.id, v.puntaje
            """)
    List<Object[]> distribucionVisiblesPorPublicador(
            @Param("perfilPublicadorIds") List<Long> perfilPublicadorIds
    );

    /** Las visibles de todas las actividades del publicador, paginadas. */
    @Query("""
            SELECT v
              FROM Valoracion v, Actividad a
             WHERE a.id = v.actividadId
               AND a.perfilPublicador.id = :perfilPublicadorId
               AND v.estado = 'VISIBLE'
             ORDER BY v.createdAt DESC
            """)
    Page<Valoracion> visiblesDePublicador(
            @Param("perfilPublicadorId") Long perfilPublicadorId,
            Pageable pageable
    );
}
