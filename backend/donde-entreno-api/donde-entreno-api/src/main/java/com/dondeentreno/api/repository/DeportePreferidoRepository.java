package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.DeportePreferido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Deportes preferidos por usuario (script 20).
 */
public interface DeportePreferidoRepository
        extends JpaRepository<DeportePreferido, Long> {

    /**
     * Slugs de los deportes del usuario, en el orden en que los eligio.
     * Query escalar con join: la entidad no navega relaciones.
     */
    @Query("""
            SELECT d.slug
              FROM DeportePreferido p
              JOIN Deporte d ON d.id = p.deporteId
             WHERE p.usuarioId = :usuarioId
             ORDER BY p.createdAt ASC, p.id ASC
            """)
    List<String> slugsDe(@Param("usuarioId") Long usuarioId);

    /** Reemplazo del conjunto: primero se vacia, despues se insertan los nuevos. */
    @Modifying
    @Query("DELETE FROM DeportePreferido p WHERE p.usuarioId = :usuarioId")
    int borrarDe(@Param("usuarioId") Long usuarioId);
}
