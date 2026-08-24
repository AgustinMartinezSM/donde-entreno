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
     * Todas las imágenes de un perfil publicador (incluidas pendientes
     * y rechazadas): el publicador ve el estado de cada una desde su
     * perfil, igual que con las de actividad.
     */
    List<Imagen> findByPerfilPublicador_IdOrderByCreatedAtDesc(Long perfilPublicadorId);

    /**
     * Imagen puntual validando que pertenezca al perfil publicador.
     */
    java.util.Optional<Imagen> findByIdAndPerfilPublicador_Id(Long id, Long perfilPublicadorId);

    /**
     * LOGO o PORTADA activa vigente de un perfil: al aprobar una nueva,
     * la anterior del mismo tipo se desactiva lógicamente (el perfil
     * tiene un solo logo y una sola portada a la vez).
     */
    List<Imagen> findByPerfilPublicador_IdAndTipoImagenAndActivaTrue(
            Long perfilPublicadorId,
            String tipoImagen
    );

    /**
     * Imágenes PRINCIPAL visibles en público de un conjunto de
     * actividades (activas Y aprobadas), en un solo query para
     * enriquecer listados paginados sin caer en N+1.
     */
    List<Imagen> findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
            String estadoModeracion,
            String tipoImagen,
            java.util.Collection<Long> actividadIds
    );

    /**
     * Logos aprobados por lote de perfiles (identidad única del
     * publicador en cards y listados): un query batch, sin N+1.
     */
    List<Imagen> findByActivaTrueAndEstadoModeracionAndTipoImagenAndPerfilPublicador_IdInOrderByOrdenAsc(
            String estadoModeracion,
            String tipoImagen,
            java.util.Collection<Long> perfilPublicadorIds
    );

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

    /**
     * TODAS las fotos visibles de un publicador (Fase 5): las que
     * cuelgan del perfil y las de sus actividades vivas, en UN query.
     * Mata el fan-out de una llamada por actividad que hacía el perfil
     * público (hasta 6 requests por vista).
     *
     * LOGO y PORTADA quedan fuera: son identidad, ya se ven en la
     * cabecera, y contarlas haría que el número de la cabecera no
     * coincidiera con lo que muestra el tab Fotos.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT i
              FROM Imagen i
             WHERE i.activa = true
               AND i.estadoModeracion = :estadoModeracion
               AND i.tipoImagen NOT IN ('LOGO', 'PORTADA')
               AND (
                     i.perfilPublicador.id = :perfilId
                  OR (i.actividad.perfilPublicador.id = :perfilId
                      AND i.actividad.activa = true
                      AND i.actividad.deletedAt IS NULL)
               )
             ORDER BY i.orden ASC, i.id ASC
            """)
    List<Imagen> fotosVisiblesDePublicador(
            @org.springframework.data.repository.query.Param("perfilId") Long perfilId,
            @org.springframework.data.repository.query.Param("estadoModeracion")
            String estadoModeracion
    );

    /**
     * La misma cuenta que arriba pero agrupada para VARIOS perfiles
     * (stats del listado sin N+1). Filas: [perfilId, cantidad].
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(i.perfilPublicador.id, i.actividad.perfilPublicador.id), COUNT(i)
              FROM Imagen i
             WHERE i.activa = true
               AND i.estadoModeracion = :estadoModeracion
               AND i.tipoImagen NOT IN ('LOGO', 'PORTADA')
               AND (
                     i.perfilPublicador.id IN :perfilIds
                  OR (i.actividad.perfilPublicador.id IN :perfilIds
                      AND i.actividad.activa = true
                      AND i.actividad.deletedAt IS NULL)
               )
             GROUP BY COALESCE(i.perfilPublicador.id, i.actividad.perfilPublicador.id)
            """)
    List<Object[]> contarFotosVisiblesPorPublicador(
            @org.springframework.data.repository.query.Param("perfilIds")
            java.util.List<Long> perfilIds,
            @org.springframework.data.repository.query.Param("estadoModeracion")
            String estadoModeracion
    );

    /**
     * Cuenta de GALERIA "que van a existir" en una actividad (activas
     * aprobadas + pendientes de moderación): es la base del límite de
     * fotos por actividad en la subida.
     */
    long countByActividad_IdAndTipoImagenAndActivaTrue(Long actividadId, String tipoImagen);

    long countByActividad_IdAndTipoImagenAndEstadoModeracion(
            Long actividadId,
            String tipoImagen,
            String estadoModeracion
    );

    /**
     * Pendientes totales de una actividad (anti-flood de la cola de
     * moderación).
     */
    long countByActividad_IdAndEstadoModeracion(Long actividadId, String estadoModeracion);

    /**
     * Un LOGO/PORTADA pendiente por vez: mientras hay uno en la cola,
     * no se acepta otro del mismo tipo (reemplazarlo = retirar el
     * pendiente y subir de nuevo).
     */
    boolean existsByPerfilPublicador_IdAndTipoImagenAndEstadoModeracion(
            Long perfilPublicadorId,
            String tipoImagen,
            String estadoModeracion
    );
}
