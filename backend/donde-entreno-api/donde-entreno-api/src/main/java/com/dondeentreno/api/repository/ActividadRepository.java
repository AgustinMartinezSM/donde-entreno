package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository de Actividad.
 *
 * Esta interfaz se encarga de consultar la tabla actividad
 * usando Spring Data JPA.
 *
 * Actividad es una de las tablas principales del MVP,
 * porque representa las propuestas deportivas que el usuario
 * final va a buscar en DondeEntreno.
 */
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    boolean existsBySlug(String slug);

    /**
     * Cuenta las actividades activas y publicadas de un perfil
     * publicador (métricas del panel del publicador).
     */
    long countByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
            Long perfilPublicadorId,
            String estadoPublicacion
    );

    @EntityGraph(attributePaths = {
            "perfilPublicador",
            "deporte",
            "deporte.categoriaDeportiva",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.barrio"
    })
    /**
     * Listado del panel del publicador (fase 6): el dueño ve sus
     * actividades publicadas Y pausadas — la pausa la maneja el, asi
     * que tiene que poder verla.
     */
    Page<Actividad> findByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
            Long perfilPublicadorId,
            java.util.Collection<String> estadosPublicacion,
            Pageable pageable
    );

    /**
     * Actividad del panel por id (fase 6): mismo criterio que el
     * listado — publicada o pausada, siempre del dueño.
     */
    Optional<Actividad> findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
            Long id,
            Long perfilPublicadorId,
            java.util.Collection<String> estadosPublicacion
    );

    Page<Actividad> findByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
            Long perfilPublicadorId,
            String estadoPublicacion,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "perfilPublicador",
            "deporte",
            "deporte.categoriaDeportiva",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.barrio"
    })
    Optional<Actividad> findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
            Long id,
            Long perfilPublicadorId,
            String estadoPublicacion
    );

    /**
     * Busca actividades activas y publicadas.
     *
     * Esto representa el listado público principal.
     *
     * Spring interpreta:
     * WHERE activa = true
     * AND estado_publicacion = 'PUBLICADA'
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionOrderByCreatedAtDesc(String estadoPublicacion);

    /**
     * Actividades publicadas de un conjunto de perfiles publicadores,
     * más recientes primero (feed de novedades de la capa social:
     * el Pageable acota al tope del feed).
     */
    @EntityGraph(attributePaths = {
            "perfilPublicador",
            "deporte",
            "deporte.categoriaDeportiva",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.barrio"
    })
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndDeletedAtIsNullAndPerfilPublicador_IdInOrderByCreatedAtDesc(
            String estadoPublicacion,
            List<Long> perfilPublicadorIds,
            Pageable pageable
    );

    /**
     * Busca una actividad activa y publicada por su slug.
     *
     * Esto nos va a servir para el detalle público de una actividad.
     *
     * Ejemplo futuro:
     * GET /api/actividades/boxeo-recreativo-para-adultos
     */
    Optional<Actividad> findBySlugAndActivaTrueAndEstadoPublicacion(
            String slug,
            String estadoPublicacion
    );

    /**
     * Actividades publicables de un conjunto de ids (los favoritos del
     * usuario): mismo criterio de visibilidad y mismo grafo que el feed,
     * así un favorito se ve idéntico a cualquier card pública. Una
     * actividad despublicada simplemente no vuelve (el favorito queda en
     * su tabla por si se republica).
     */
    @EntityGraph(attributePaths = {
            "perfilPublicador",
            "deporte",
            "deporte.categoriaDeportiva",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.barrio"
    })
    List<Actividad> findByIdInAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
            Collection<Long> ids,
            String estadoPublicacion
    );

    /**
     * Busca actividades públicas filtrando por deporte.
     *
     * Ejemplo:
     * GET /api/actividades?deporteId=1
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndDeporte_IdOrderByCreatedAtDesc(
            String estadoPublicacion,
            Long deporteId
    );

    /**
     * Busca actividades públicas filtrando por slug de deporte.
     *
     * Ejemplo:
     * GET /api/actividades?deporteSlug=boxeo
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndDeporte_SlugOrderByCreatedAtDesc(
            String estadoPublicacion,
            String deporteSlug
    );

    /**
     * Busca actividades públicas filtrando por ciudad.
     *
     * Como Actividad se relaciona con Ubicacion,
     * y Ubicacion se relaciona con Ciudad, usamos:
     *
     * ubicacion_Ciudad_Id
     *
     * Esto significa:
     * entrar a ubicacion, después a ciudad, y filtrar por su id.
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndUbicacion_Ciudad_IdOrderByCreatedAtDesc(
            String estadoPublicacion,
            Long ciudadId
    );

    /**
     * Busca actividades públicas filtrando por barrio.
     *
     * Similar al filtro de ciudad, pero entrando a ubicacion -> barrio.
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndUbicacion_Barrio_IdOrderByCreatedAtDesc(
            String estadoPublicacion,
            Long barrioId
    );

    /**
     * Busca actividades públicas filtrando por perfil publicador.
     *
     * Ejemplo:
     * actividades de un club, gimnasio o profesor determinado.
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndPerfilPublicador_IdOrderByCreatedAtDesc(
            String estadoPublicacion,
            Long perfilPublicadorId
    );

    /**
     * Busca actividades públicas filtrando por nivel.
     *
     * Valores posibles según la base:
     * PRINCIPIANTE, INTERMEDIO, AVANZADO, TODOS.
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndNivelOrderByCreatedAtDesc(
            String estadoPublicacion,
            String nivel
    );

    /**
     * Busca actividades públicas filtrando por modalidad.
     *
     * Valores posibles según la base:
     * PRESENCIAL, ONLINE, MIXTA.
     */
    List<Actividad> findByActivaTrueAndEstadoPublicacionAndModalidadOrderByCreatedAtDesc(
            String estadoPublicacion,
            String modalidad
    );

    /**
     * Busca actividades públicas aplicando filtros combinados.
     *
     * Todos los filtros son opcionales.
     * Si un parámetro viene en null, ese filtro no se aplica.
     *
     * El filtro texto busca coincidencias en:
     * - título de la actividad
     * - descripción de la actividad
     * - nombre del deporte
     * - nombre del perfil publicador
     * - nombre de la ciudad
     * - nombre del barrio
     *
     * La comparación es insensible a mayúsculas y a tildes
     * (unaccent), de modo que "futbol" encuentra "Fútbol".
     * Requiere la extensión unaccent y su registro en Hibernate
     * (ver UnaccentFunctionContributor y migración 16).
     */
    @Query("""
        SELECT a
        FROM Actividad a
        WHERE a.activa = true
          AND a.estadoPublicacion = :estadoPublicacion
          AND (:deporteId IS NULL OR a.deporte.id = :deporteId)
          AND (:deporteSlug IS NULL OR a.deporte.slug = :deporteSlug)
          AND (:ciudadId IS NULL OR a.ubicacion.ciudad.id = :ciudadId)
          AND (:ciudadSlug IS NULL OR a.ubicacion.ciudad.slug = :ciudadSlug)
          AND (:barrioId IS NULL OR a.ubicacion.barrio.id = :barrioId)
          AND (:perfilPublicadorId IS NULL OR a.perfilPublicador.id = :perfilPublicadorId)
          AND (:nivel IS NULL OR a.nivel = :nivel)
          AND (:modalidad IS NULL OR a.modalidad = :modalidad)
          AND (
                :texto IS NULL
                OR unaccent(LOWER(a.titulo)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.descripcion)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.deporte.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.perfilPublicador.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.ubicacion.ciudad.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.ubicacion.barrio.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
          )
        ORDER BY a.createdAt DESC
        """)
    List<Actividad> buscarActividadesPublicadasConFiltros(
            @Param("estadoPublicacion") String estadoPublicacion,
            @Param("deporteId") Long deporteId,
            @Param("deporteSlug") String deporteSlug,
            @Param("ciudadId") Long ciudadId,
            @Param("ciudadSlug") String ciudadSlug,
            @Param("barrioId") Long barrioId,
            @Param("perfilPublicadorId") Long perfilPublicadorId,
            @Param("nivel") String nivel,
            @Param("modalidad") String modalidad,
            @Param("texto") String texto
    );

    /**
     * Busca actividades públicas aplicando filtros combinados
     * y devolviendo resultados paginados.
     *
     * Todos los filtros son opcionales.
     * Si un parámetro viene en null, ese filtro no se aplica.
     *
     * El filtro texto busca coincidencias en:
     * - título de la actividad
     * - descripción de la actividad
     * - nombre del deporte
     * - nombre del perfil publicador
     * - nombre de la ciudad
     * - nombre del barrio
     *
     * La comparación es insensible a mayúsculas y a tildes
     * (unaccent), de modo que "futbol" encuentra "Fútbol".
     * Requiere la extensión unaccent y su registro en Hibernate
     * (ver UnaccentFunctionContributor y migración 16).
     */
    @Query("""
        SELECT a
        FROM Actividad a
        WHERE a.activa = true
          AND a.estadoPublicacion = :estadoPublicacion
          AND (:deporteId IS NULL OR a.deporte.id = :deporteId)
          AND (:deporteSlug IS NULL OR a.deporte.slug = :deporteSlug)
          AND (:ciudadId IS NULL OR a.ubicacion.ciudad.id = :ciudadId)
          AND (:ciudadSlug IS NULL OR a.ubicacion.ciudad.slug = :ciudadSlug)
          AND (:barrioId IS NULL OR a.ubicacion.barrio.id = :barrioId)
          AND (:perfilPublicadorId IS NULL OR a.perfilPublicador.id = :perfilPublicadorId)
          AND (:nivel IS NULL OR a.nivel = :nivel)
          AND (:modalidad IS NULL OR a.modalidad = :modalidad)
          AND (
                :texto = ''
                OR unaccent(LOWER(a.titulo)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.descripcion)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.deporte.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.perfilPublicador.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.ubicacion.ciudad.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
                OR unaccent(LOWER(a.ubicacion.barrio.nombre)) LIKE unaccent(LOWER(CONCAT('%', :texto, '%'))) ESCAPE '\\'
          )
        """)
    Page<Actividad> buscarActividadesPublicadasConFiltrosPaginado(
            @Param("estadoPublicacion") String estadoPublicacion,
            @Param("deporteId") Long deporteId,
            @Param("deporteSlug") String deporteSlug,
            @Param("ciudadId") Long ciudadId,
            @Param("ciudadSlug") String ciudadSlug,
            @Param("barrioId") Long barrioId,
            @Param("perfilPublicadorId") Long perfilPublicadorId,
            @Param("nivel") String nivel,
            @Param("modalidad") String modalidad,
            @Param("texto") String texto,
            Pageable pageable
    );

}
