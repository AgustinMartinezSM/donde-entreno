package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.SolicitudPublicacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository de SolicitudPublicacion.
 *
 * Esta interfaz se encarga de consultar la tabla solicitud_publicacion
 * usando Spring Data JPA.
 */
public interface SolicitudPublicacionRepository extends JpaRepository<SolicitudPublicacion, Long> {

    /**
     * Verifica si ya existe una solicitud con el codigo de seguimiento indicado.
     *
     * Esto permite evitar colisiones al generar codigos publicos.
     */
    boolean existsByCodigoSeguimiento(String codigoSeguimiento);

    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio"
    })
    Page<SolicitudPublicacion> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio"
    })
    Page<SolicitudPublicacion> findByEstadoAndDeletedAtIsNull(String estado, Pageable pageable);

    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio",
            "actividadGenerada"
    })
    Page<SolicitudPublicacion> findByUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
            Long usuarioId,
            Long perfilPublicadorId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio",
            "actividadGenerada"
    })
    Page<SolicitudPublicacion> findByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
            Long usuarioId,
            Long perfilPublicadorId,
            String estado,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio",
            "usuario",
            "perfilPublicador",
            "perfilPublicador.usuario",
            "revisadoPorUsuario",
            "revisadoPorUsuario.rol",
            "actividadGenerada"
    })
    Optional<SolicitudPublicacion> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio",
            "actividadGenerada"
    })
    Optional<SolicitudPublicacion> findByIdAndUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
            Long id,
            Long usuarioId,
            Long perfilPublicadorId
    );

    Optional<SolicitudPublicacion> findByActividadGenerada_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
            Long actividadGeneradaId,
            Long perfilPublicadorId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "deporte",
            "ciudad",
            "barrio",
            "revisadoPorUsuario",
            "revisadoPorUsuario.rol",
            "actividadGenerada"
    })
    @Query("""
            SELECT s
            FROM SolicitudPublicacion s
            WHERE s.id = :id
              AND s.deletedAt IS NULL
            """)
    Optional<SolicitudPublicacion> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
