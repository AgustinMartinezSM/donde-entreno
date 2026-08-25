package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.MeGustaNovedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Reacciones a novedades (script 37).
 */
public interface MeGustaNovedadRepository extends JpaRepository<MeGustaNovedad, Long> {

    Optional<MeGustaNovedad> findByUsuarioIdAndNovedadId(Long usuarioId, Long novedadId);

    boolean existsByUsuarioIdAndNovedadId(Long usuarioId, Long novedadId);

    long countByNovedadId(Long novedadId);

    /** Conteo agrupado para pintar una lista sin N+1. */
    @Query("""
            SELECT m.novedadId AS novedadId, COUNT(m) AS cantidad
              FROM MeGustaNovedad m
             WHERE m.novedadId IN :novedadIds
             GROUP BY m.novedadId
            """)
    List<ConteoMeGusta> contarPorNovedades(@Param("novedadIds") List<Long> novedadIds);

    /** Las que ya marcó el usuario, para no pedirlas una por una. */
    @Query("""
            SELECT m.novedadId FROM MeGustaNovedad m
             WHERE m.usuarioId = :usuarioId
               AND m.novedadId IN :novedadIds
            """)
    List<Long> novedadIdsConMeGustaDe(
            @Param("usuarioId") Long usuarioId,
            @Param("novedadIds") List<Long> novedadIds
    );

    /** Proyección del conteo agrupado. */
    interface ConteoMeGusta {
        Long getNovedadId();

        Long getCantidad();
    }
}
