package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository de Imagen.
 *
 * Esta interfaz se encarga de consultar la tabla imagen
 * usando Spring Data JPA.
 */
public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    /**
     * Busca imágenes activas de una actividad por ID,
     * ordenadas por el campo orden.
     *
     * Ejemplo:
     * imágenes de galería o imagen principal de una actividad.
     */
    List<Imagen> findByActivaTrueAndActividad_IdOrderByOrdenAsc(Long actividadId);

    /**
     * Busca imágenes activas de una actividad por slug,
     * ordenadas por el campo orden.
     *
     * Esto nos va a servir para:
     * GET /api/actividades/{slug}/imagenes
     */
    List<Imagen> findByActivaTrueAndActividad_SlugOrderByOrdenAsc(String actividadSlug);

    /**
     * Busca imágenes activas de una actividad por slug y tipo.
     *
     * Ejemplo:
     * tipoImagen = PRINCIPAL
     * tipoImagen = GALERIA
     */
    List<Imagen> findByActivaTrueAndActividad_SlugAndTipoImagenOrderByOrdenAsc(
            String actividadSlug,
            String tipoImagen
    );

    /**
     * Busca imágenes activas de un perfil publicador por ID,
     * ordenadas por el campo orden.
     *
     * Ejemplo:
     * logo o portada de un club, gimnasio o profesor.
     */
    List<Imagen> findByActivaTrueAndPerfilPublicador_IdOrderByOrdenAsc(Long perfilPublicadorId);

    /**
     * Busca imágenes activas de un perfil publicador por ID y tipo.
     *
     * Ejemplo:
     * tipoImagen = LOGO
     * tipoImagen = PORTADA
     */
    List<Imagen> findByActivaTrueAndPerfilPublicador_IdAndTipoImagenOrderByOrdenAsc(
            Long perfilPublicadorId,
            String tipoImagen
    );
}
