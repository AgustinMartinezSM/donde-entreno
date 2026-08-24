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
}
