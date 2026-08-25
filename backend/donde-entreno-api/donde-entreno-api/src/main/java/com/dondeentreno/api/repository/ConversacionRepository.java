package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Conversaciones del inbox (script 36).
 */
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    /** La bandeja del usuario, la más reciente primero. */
    List<Conversacion> findByUsuarioIdOrderByUltimoMensajeAtDesc(Long usuarioId);

    /** La bandeja del publicador. */
    List<Conversacion> findByPerfilPublicadorIdOrderByUltimoMensajeAtDesc(
            Long perfilPublicadorId
    );

    /**
     * La conversación existente para ese par, si la hay.
     *
     * Consulta explícita y no un `findBy...` derivado porque
     * `actividadId` es nullable: Spring Data generaría `= :actividadId`
     * y NULL nunca es igual a NULL, así que abriría un hilo nuevo cada
     * vez que alguien consulta por el club en general. Es la misma
     * razón por la que la unicidad son dos índices parciales.
     */
    @Query("""
            SELECT c FROM Conversacion c
             WHERE c.usuarioId = :usuarioId
               AND c.perfilPublicadorId = :perfilPublicadorId
               AND ((:actividadId IS NULL AND c.actividadId IS NULL)
                    OR c.actividadId = :actividadId)
            """)
    Optional<Conversacion> buscarExistente(
            @Param("usuarioId") Long usuarioId,
            @Param("perfilPublicadorId") Long perfilPublicadorId,
            @Param("actividadId") Long actividadId
    );

    /** Tope diario: cuántas conversaciones abrió hoy ese usuario. */
    long countByUsuarioIdAndCreatedAtGreaterThanEqual(
            Long usuarioId,
            OffsetDateTime desde
    );
}
