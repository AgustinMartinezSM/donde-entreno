package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.ComentarioAviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Comentarios sobre avisos del grupo (script 38).
 */
public interface ComentarioAvisoRepository extends JpaRepository<ComentarioAviso, Long> {

    List<ComentarioAviso> findByAvisoIdOrderByCreatedAtAsc(Long avisoId);

    /** Conteo agrupado para pintar la lista de avisos sin N+1. */
    @Query("""
            SELECT c.avisoId AS avisoId, COUNT(c) AS cantidad
              FROM ComentarioAviso c
             WHERE c.avisoId IN :avisoIds
               AND c.estado = 'VISIBLE'
             GROUP BY c.avisoId
            """)
    List<ConteoComentarios> contarPorAvisos(@Param("avisoIds") List<Long> avisoIds);

    /** Tope diario del usuario (mismo criterio que los comentarios de fotos). */
    long countByUsuarioIdAndCreatedAtGreaterThanEqual(
            Long usuarioId,
            OffsetDateTime desde
    );

    interface ConteoComentarios {
        Long getAvisoId();

        Long getCantidad();
    }
}
