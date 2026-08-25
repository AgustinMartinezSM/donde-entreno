package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.EventoDeportivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Eventos deportivos (script 35).
 */
public interface EventoDeportivoRepository extends JpaRepository<EventoDeportivo, Long> {

    Optional<EventoDeportivo> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * El calendario público: lo que viene, más cerca primero.
     *
     * Los filtros opcionales usan `(:x IS NULL OR ...)`, que es el
     * patrón del resto del proyecto. La ciudad y el barrio salen del
     * join con `ubicacion`, que es justamente por qué `ubicacion_id`
     * es NOT NULL.
     *
     * `CANCELADO` NO entra al calendario: quien no se enteró no
     * necesita ver la cancelación en una agenda de lo que viene. El
     * detalle, en cambio, sigue vivo para quien tenga el link.
     */
    @Query("""
            SELECT e FROM EventoDeportivo e, Ubicacion u
             WHERE u.id = e.ubicacionId
               AND e.estado = 'PUBLICADO'
               AND e.iniciaAt >= :desde
               AND (:hasta IS NULL OR e.iniciaAt < :hasta)
               AND (:ciudadId IS NULL OR u.ciudad.id = :ciudadId)
               AND (:ciudadSlug IS NULL OR u.ciudad.slug = :ciudadSlug)
               AND (:barrioId IS NULL OR u.barrio.id = :barrioId)
               AND (:deporteId IS NULL OR e.deporteId = :deporteId)
             ORDER BY e.iniciaAt ASC
            """)
    Page<EventoDeportivo> buscarEnCalendario(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("ciudadId") Long ciudadId,
            @Param("ciudadSlug") String ciudadSlug,
            @Param("barrioId") Long barrioId,
            @Param("deporteId") Long deporteId,
            Pageable pageable
    );

    /**
     * Los próximos de un publicador (solapa de su perfil público).
     * Incluye los CANCELADOS: si alguien entra al perfil a ver el
     * torneo del sábado, enterarse de que se canceló es el dato.
     */
    @Query("""
            SELECT e FROM EventoDeportivo e
             WHERE e.perfilPublicadorId = :perfilPublicadorId
               AND e.estado IN ('PUBLICADO', 'CANCELADO')
               AND e.iniciaAt >= :desde
             ORDER BY e.iniciaAt ASC
            """)
    List<EventoDeportivo> proximosDePerfil(
            @Param("perfilPublicadorId") Long perfilPublicadorId,
            @Param("desde") OffsetDateTime desde,
            Pageable pageable
    );

    /** El aviso "hay algo próximo" en el detalle de la actividad. */
    @Query("""
            SELECT e FROM EventoDeportivo e
             WHERE e.actividadId = :actividadId
               AND e.estado = 'PUBLICADO'
               AND e.iniciaAt >= :desde
             ORDER BY e.iniciaAt ASC
            """)
    List<EventoDeportivo> proximosDeActividad(
            @Param("actividadId") Long actividadId,
            @Param("desde") OffsetDateTime desde,
            Pageable pageable
    );

    /** La agenda del publicador: todo lo suyo salvo lo que borró. */
    List<EventoDeportivo> findByPerfilPublicadorIdAndEstadoNotOrderByIniciaAtDesc(
            Long perfilPublicadorId,
            String estado
    );

    /**
     * Tope diario de campanita: cuántos publicó hoy. Cuenta TODOS
     * —incluso los borrados— para que borrar y volver a publicar no
     * sea la forma de saltear el límite (lección del script 34).
     */
    long countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
            Long perfilPublicadorId,
            OffsetDateTime desde
    );
}
