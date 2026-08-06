package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.SeguimientoPublicador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
