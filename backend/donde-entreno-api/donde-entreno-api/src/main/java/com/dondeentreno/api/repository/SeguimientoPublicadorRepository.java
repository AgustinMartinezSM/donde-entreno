package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.SeguimientoPublicador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Seguimientos usuario -> perfil publicador (capa social, Bloque 8).
 */
public interface SeguimientoPublicadorRepository
        extends JpaRepository<SeguimientoPublicador, Long> {

    boolean existsByUsuario_IdAndPerfilPublicador_Id(Long usuarioId, Long perfilPublicadorId);

    /**
     * Cantidad de usuarios que siguen a un perfil publicador
     * (métrica "seguidores" del panel del publicador).
     */
    long countByPerfilPublicador_Id(Long perfilPublicadorId);

    /**
     * Publicadores que sigue un usuario, más recientes primero.
     * Trae el perfil y su ciudad para armar el DTO sin N+1.
     */
    @EntityGraph(attributePaths = {"perfilPublicador", "perfilPublicador.ciudadPrincipal"})
    List<SeguimientoPublicador> findByUsuario_IdOrderByCreatedAtDesc(Long usuarioId);

    /**
     * Deja de seguir: borra la fila si existe (idempotente). Devuelve
     * la cantidad borrada (0 si no lo seguía).
     */
    long deleteByUsuario_IdAndPerfilPublicador_Id(Long usuarioId, Long perfilPublicadorId);

    /**
     * Seguidores de un conjunto de perfiles, en un solo query, para
     * enriquecer el listado público sin caer en N+1.
     *
     * Los perfiles sin seguidores no aparecen en el resultado: el
     * GROUP BY solo devuelve los que tienen al menos una fila, así que
     * el caller completa con cero.
     */
    @Query("""
            SELECT s.perfilPublicador.id AS perfilPublicadorId, COUNT(s) AS cantidad
            FROM SeguimientoPublicador s
            WHERE s.perfilPublicador.id IN :perfilPublicadorIds
            GROUP BY s.perfilPublicador.id
            """)
    List<ConteoSeguidores> contarSeguidoresPorPerfiles(
            @Param("perfilPublicadorIds") Collection<Long> perfilPublicadorIds
    );

    /**
     * Proyección del conteo agrupado.
     */
    interface ConteoSeguidores {
        Long getPerfilPublicadorId();

        long getCantidad();
    }
}
