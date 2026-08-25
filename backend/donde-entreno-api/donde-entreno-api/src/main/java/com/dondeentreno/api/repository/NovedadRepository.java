package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Novedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Novedades del canal (script 34).
 */
public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    /** Las visibles de un publicador, más nuevas primero (público). */
    Page<Novedad> findByPerfilPublicadorIdAndEstadoOrderByCreatedAtDesc(
            Long perfilPublicadorId,
            String estado,
            Pageable pageable
    );

    /** Todas las del publicador salvo las que él mismo eliminó (panel). */
    List<Novedad> findByPerfilPublicadorIdAndEstadoNotOrderByCreatedAtDesc(
            Long perfilPublicadorId,
            String estado
    );

    /**
     * Tope diario del canal: cuántas publicó hoy. Cuenta TODAS —
     * incluso las que borró— para que borrar y volver a publicar no
     * sea una forma de saltear el límite.
     */
    long countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
            Long perfilPublicadorId,
            OffsetDateTime desde
    );
}
