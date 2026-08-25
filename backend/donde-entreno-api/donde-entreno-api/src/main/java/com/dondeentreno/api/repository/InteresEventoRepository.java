package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.InteresEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * "Me interesa" sobre eventos (script 35).
 */
public interface InteresEventoRepository extends JpaRepository<InteresEvento, Long> {

    Optional<InteresEvento> findByUsuarioIdAndEventoDeportivoId(
            Long usuarioId,
            Long eventoDeportivoId
    );

    boolean existsByUsuarioIdAndEventoDeportivoId(Long usuarioId, Long eventoDeportivoId);

    long countByEventoDeportivoId(Long eventoDeportivoId);

    /**
     * Conteo AGRUPADO para pintar una lista de eventos sin N+1 (mismo
     * patrón que el contador de seguidores del perfil).
     */
    @Query("""
            SELECT i.eventoDeportivoId AS eventoId, COUNT(i) AS cantidad
              FROM InteresEvento i
             WHERE i.eventoDeportivoId IN :eventoIds
             GROUP BY i.eventoDeportivoId
            """)
    List<ConteoInteresados> contarPorEventos(@Param("eventoIds") List<Long> eventoIds);

    /** Los eventos que le interesan a un usuario, para marcarlos. */
    @Query("""
            SELECT i.eventoDeportivoId FROM InteresEvento i
             WHERE i.usuarioId = :usuarioId
               AND i.eventoDeportivoId IN :eventoIds
            """)
    List<Long> eventoIdsConInteresDe(
            @Param("usuarioId") Long usuarioId,
            @Param("eventoIds") List<Long> eventoIds
    );

    /** Proyección del conteo agrupado. */
    interface ConteoInteresados {
        Long getEventoId();

        Long getCantidad();
    }
}
