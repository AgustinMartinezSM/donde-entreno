package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.ComentarioImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Comentarios en fotos (script 30).
 */
public interface ComentarioImagenRepository
        extends JpaRepository<ComentarioImagen, Long> {

    List<ComentarioImagen> findByImagenIdAndEstadoOrderByCreatedAtAsc(
            Long imagenId,
            String estado
    );

    /** Tope diario por usuario. */
    long countByUsuarioIdAndCreatedAtGreaterThanEqual(
            Long usuarioId,
            OffsetDateTime desde
    );

    /**
     * Conteo de visibles por foto en UN query agrupado (patrón likes):
     * sin N+1 en las galerías.
     */
    @Query("""
            SELECT c.imagenId, COUNT(c)
              FROM ComentarioImagen c
             WHERE c.imagenId IN :imagenIds
               AND c.estado = 'VISIBLE'
             GROUP BY c.imagenId
            """)
    List<Object[]> contarVisiblesPorImagen(
            @Param("imagenIds") Collection<Long> imagenIds
    );
}
