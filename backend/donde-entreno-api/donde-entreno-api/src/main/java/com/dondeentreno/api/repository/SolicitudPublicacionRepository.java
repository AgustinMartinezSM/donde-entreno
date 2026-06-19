package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.SolicitudPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
