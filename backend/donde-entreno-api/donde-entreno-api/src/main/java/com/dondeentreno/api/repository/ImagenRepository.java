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
     * Imágenes visibles en público de una actividad por slug:
     * activas Y aprobadas por moderación.
     *
     * GET /api/actividades/{slug}/imagenes
     */
    List<Imagen> findByActivaTrueAndEstadoModeracionAndActividad_SlugOrderByOrdenAsc(
            String estadoModeracion,
            String actividadSlug
    );

    /**
     * Imágenes visibles en público de una actividad por slug y tipo
     * (PRINCIPAL o GALERIA): activas Y aprobadas por moderación.
     */
    List<Imagen> findByActivaTrueAndEstadoModeracionAndActividad_SlugAndTipoImagenOrderByOrdenAsc(
            String estadoModeracion,
            String actividadSlug,
            String tipoImagen
    );

    /**
     * Imágenes visibles en público de un perfil publicador:
     * activas Y aprobadas por moderación.
     */
    List<Imagen> findByActivaTrueAndEstadoModeracionAndPerfilPublicador_IdOrderByOrdenAsc(
            String estadoModeracion,
            Long perfilPublicadorId
    );

    /**
     * Imágenes visibles en público de un perfil publicador por tipo
     * (LOGO o PORTADA): activas Y aprobadas por moderación.
     */
    List<Imagen> findByActivaTrueAndEstadoModeracionAndPerfilPublicador_IdAndTipoImagenOrderByOrdenAsc(
            String estadoModeracion,
            Long perfilPublicadorId,
            String tipoImagen
    );

    /**
     * Cola de moderación del admin filtrada por estado
     * (el orden lo define el Pageable: más antiguas primero).
     */
    org.springframework.data.domain.Page<Imagen> findByEstadoModeracion(
            String estadoModeracion,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Imagen PRINCIPAL activa vigente de una actividad: al aprobar una
     * nueva PRINCIPAL, la anterior se desactiva lógicamente.
     */
    List<Imagen> findByActividad_IdAndTipoImagenAndActivaTrue(
            Long actividadId,
            String tipoImagen
    );

    /**
     * Todas las imágenes de una actividad (incluidas pendientes y
     * rechazadas): el panel del publicador muestra el estado de cada una.
     */
    List<Imagen> findByActividad_IdOrderByCreatedAtDesc(Long actividadId);

    /**
     * Imagen puntual validando que pertenezca a la actividad
     * (ownership de la actividad ya validado por el service).
     */
    java.util.Optional<Imagen> findByIdAndActividad_Id(Long id, Long actividadId);

    /**
     * Cuenta las imágenes de las actividades de un perfil publicador en
     * un estado de moderación dado (métricas del panel: se usa con
     * PENDIENTE). Las imágenes que sube el publicador cuelgan de la
     * actividad (perfil_publicador_id queda null por la constraint
     * chk_imagen_duenio_unico), por eso se cuenta vía actividad. Sin
     * filtro de activa: nacen PENDIENTE y activa=false hasta aprobarse.
     */
    long countByEstadoModeracionAndActividad_PerfilPublicador_Id(
            String estadoModeracion,
            Long perfilPublicadorId
    );
}
