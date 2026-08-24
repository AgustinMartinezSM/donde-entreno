package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.FeedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Línea de tiempo de hechos de los publicadores (script 32).
 */
public interface FeedEventRepository extends JpaRepository<FeedEvent, Long> {

    /**
     * El feed de un seguidor: los hechos de los perfiles que sigue,
     * más nuevos primero. Paginado de verdad (la deuda que dejó el
     * feed V1, que cortaba en 20 sin forma de pedir más).
     */
    Page<FeedEvent> findByPerfilPublicadorIdInOrderByCreatedAtDesc(
            Collection<Long> perfilPublicadorIds,
            Pageable pageable
    );

    /** Los hechos de UN publicador (su perfil público, a futuro). */
    Page<FeedEvent> findByPerfilPublicadorIdOrderByCreatedAtDesc(
            Long perfilPublicadorId,
            Pageable pageable
    );
}
