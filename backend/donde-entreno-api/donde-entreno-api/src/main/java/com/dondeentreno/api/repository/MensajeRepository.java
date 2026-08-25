package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mensajes del inbox (script 36).
 */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /** El hilo completo, en orden. */
    List<Mensaje> findByConversacionIdOrderByCreatedAtAsc(Long conversacionId);

    /**
     * Los últimos mensajes de un hilo, del más nuevo al más viejo.
     *
     * Es el ÚNICO camino por el que el admin ve algo de una
     * conversación privada: el mensaje reportado con un contexto
     * mínimo. Nunca el hilo entero.
     */
    List<Mensaje> findTop3ByConversacionIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            Long conversacionId,
            OffsetDateTime hasta
    );

    /** El contador de no leídos de una bandeja, agrupado (sin N+1). */
    @Query("""
            SELECT m.conversacionId AS conversacionId, COUNT(m) AS cantidad
              FROM Mensaje m
             WHERE m.conversacionId IN :conversacionIds
               AND m.autor <> :autorPropio
               AND m.leidoAt IS NULL
               AND m.estado = 'VISIBLE'
             GROUP BY m.conversacionId
            """)
    List<ConteoNoLeidos> contarNoLeidos(
            @Param("conversacionIds") List<Long> conversacionIds,
            @Param("autorPropio") String autorPropio
    );

    /**
     * Marca leído todo lo que escribió el OTRO en ese hilo, en un solo
     * UPDATE (mismo patrón que `marcarTodasLeidas` de notificaciones).
     */
    @Modifying
    @Query("""
            UPDATE Mensaje m
               SET m.leidoAt = :ahora
             WHERE m.conversacionId = :conversacionId
               AND m.autor <> :autorPropio
               AND m.leidoAt IS NULL
            """)
    int marcarLeidos(
            @Param("conversacionId") Long conversacionId,
            @Param("autorPropio") String autorPropio,
            @Param("ahora") OffsetDateTime ahora
    );

    /**
     * El número del badge de "Mis consultas": todo lo que le escribieron
     * al usuario y todavía no leyó, en UNA query.
     *
     * Endpoint propio y no "traer la bandeja y sumar": para pintar un
     * número no hace falta resolver identidades, actividades y vistas
     * previas de cada conversación.
     */
    @Query("""
            SELECT COUNT(m) FROM Mensaje m, Conversacion c
             WHERE c.id = m.conversacionId
               AND c.usuarioId = :usuarioId
               AND m.autor = 'PUBLICADOR'
               AND m.leidoAt IS NULL
               AND m.estado = 'VISIBLE'
            """)
    long contarNoLeidosDelUsuario(@Param("usuarioId") Long usuarioId);

    /** Ídem para la bandeja del publicador. */
    @Query("""
            SELECT COUNT(m) FROM Mensaje m, Conversacion c
             WHERE c.id = m.conversacionId
               AND c.perfilPublicadorId = :perfilPublicadorId
               AND m.autor = 'USUARIO'
               AND m.leidoAt IS NULL
               AND m.estado = 'VISIBLE'
            """)
    long contarNoLeidosDelPublicador(@Param("perfilPublicadorId") Long perfilPublicadorId);

    /** Tope diario: cuántos mensajes escribió hoy ese usuario. */
    @Query("""
            SELECT COUNT(m) FROM Mensaje m, Conversacion c
             WHERE c.id = m.conversacionId
               AND c.usuarioId = :usuarioId
               AND m.autor = 'USUARIO'
               AND m.createdAt >= :desde
            """)
    long contarDelUsuarioDesde(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") OffsetDateTime desde
    );

    /** Proyección del conteo agrupado. */
    interface ConteoNoLeidos {
        Long getConversacionId();

        Long getCantidad();
    }
}
