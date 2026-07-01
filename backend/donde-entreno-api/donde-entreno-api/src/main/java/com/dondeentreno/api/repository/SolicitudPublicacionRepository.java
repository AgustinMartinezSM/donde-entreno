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
            "revisadoPorUsuario",
            "revisadoPorUsuario.rol",
            "actividadGenerada"
    })
    Optional<SolicitudPublicacion> findByIdAndDeletedAtIsNull(Long id);

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
