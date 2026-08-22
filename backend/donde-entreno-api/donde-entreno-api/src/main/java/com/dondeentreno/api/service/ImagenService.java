package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaImagenRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de Imagen.
 *
 * Esta capa contiene la lógica relacionada con imágenes
 * de actividades y perfiles publicadores.
 */
@Service
public class ImagenService {

    /**
     * Las vistas públicas solo muestran imágenes aprobadas por
     * moderación (además de activas). Las PENDIENTE/RECHAZADA solo
     * se ven en el panel del publicador y en la cola del admin.
     */
    private static final String ESTADO_MODERACION_APROBADA = "APROBADA";

    private static final String TIPO_IMAGEN_PRINCIPAL = "PRINCIPAL";

    private final ImagenRepository imagenRepository;
    private final MeGustaImagenRepository meGustaImagenRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public ImagenService(
            ImagenRepository imagenRepository,
            MeGustaImagenRepository meGustaImagenRepository
    ) {
        this.imagenRepository = imagenRepository;
        this.meGustaImagenRepository = meGustaImagenRepository;
    }

    /**
     * Suma el contador de likes (bloque 14) a una lista pública de
     * imágenes, en UN solo query agrupado — sin N+1. Fotos sin likes
     * quedan en 0 explícito: en público el dato siempre viaja.
     */
    private List<ImagenDTO> conLikes(List<ImagenDTO> imagenes) {
        if (imagenes.isEmpty()) {
            return imagenes;
        }

        List<Long> ids = imagenes.stream().map(ImagenDTO::getId).toList();
        Map<Long, Long> conteos = new HashMap<>();

        for (Object[] fila : meGustaImagenRepository.contarPorImagen(ids)) {
            conteos.put((Long) fila[0], (Long) fila[1]);
        }

        for (ImagenDTO imagen : imagenes) {
            imagen.setCantidadLikes(conteos.getOrDefault(imagen.getId(), 0L));
        }

        return imagenes;
    }

    /**
     * Obtiene imágenes visibles en público de una actividad por slug
     * (activas y aprobadas por moderación).
     *
     * @param actividadSlug slug de la actividad.
     * @return lista de imágenes visibles en formato DTO.
     */
    public List<ImagenDTO> obtenerImagenesPorActividadSlug(String actividadSlug) {
        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndEstadoModeracionAndActividad_SlugOrderByOrdenAsc(
                        ESTADO_MODERACION_APROBADA,
                        actividadSlug
                );

        return conLikes(imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList());
    }

    /**
     * Obtiene imágenes visibles en público de una actividad
     * por slug y tipo (activas y aprobadas por moderación).
     *
     * @param actividadSlug slug de la actividad.
     * @param tipoImagen tipo de imagen.
     * @return lista de imágenes visibles filtradas por tipo.
     */
    public List<ImagenDTO> obtenerImagenesPorActividadSlugYTipo(
            String actividadSlug,
            String tipoImagen
    ) {
        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndEstadoModeracionAndActividad_SlugAndTipoImagenOrderByOrdenAsc(
                        ESTADO_MODERACION_APROBADA,
                        actividadSlug,
                        tipoImagen
                );

        return conLikes(imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList());
    }

    /**
     * Obtiene imágenes visibles en público de un perfil publicador
     * (activas y aprobadas por moderación).
     *
     * @param perfilPublicadorId ID del perfil publicador.
     * @return lista de imágenes visibles en formato DTO.
     */
    public List<ImagenDTO> obtenerImagenesPorPerfilPublicador(Long perfilPublicadorId) {
        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndEstadoModeracionAndPerfilPublicador_IdOrderByOrdenAsc(
                        ESTADO_MODERACION_APROBADA,
                        perfilPublicadorId
                );

        return conLikes(imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList());
    }

    /**
     * Obtiene imágenes visibles en público de un perfil publicador
     * por tipo (activas y aprobadas por moderación).
     *
     * @param perfilPublicadorId ID del perfil publicador.
     * @param tipoImagen tipo de imagen.
     * @return lista de imágenes visibles filtradas por tipo.
     */
    public List<ImagenDTO> obtenerImagenesPorPerfilPublicadorYTipo(
            Long perfilPublicadorId,
            String tipoImagen
    ) {
        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndEstadoModeracionAndPerfilPublicador_IdAndTipoImagenOrderByOrdenAsc(
                        ESTADO_MODERACION_APROBADA,
                        perfilPublicadorId,
                        tipoImagen
                );

        return conLikes(imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList());
    }

    /**
     * Enriquece DTOs de actividad con la URL de su imagen PRINCIPAL
     * visible en público (activa y aprobada por moderación).
     *
     * Un solo query batch por lote (los listados son páginas de hasta
     * 50 actividades), sin N+1. Si una actividad tiene más de una
     * PRINCIPAL vigente, gana la de menor orden (el query ya viene
     * ordenado así). Las actividades sin imagen quedan con null y el
     * frontend cae a su ilustración por deporte.
     *
     * @param actividades DTOs ya mapeados (se mutan en el lugar).
     */
    public void asignarImagenPrincipal(List<ActividadDTO> actividades) {
        if (actividades == null || actividades.isEmpty()) {
            return;
        }

        List<Long> actividadIds = actividades.stream()
                .map(ActividadDTO::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (actividadIds.isEmpty()) {
            return;
        }

        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
                        ESTADO_MODERACION_APROBADA,
                        TIPO_IMAGEN_PRINCIPAL,
                        actividadIds
                );

        Map<Long, String> urlPorActividad = new HashMap<>();

        for (Imagen imagen : imagenes) {
            if (imagen.getActividad() == null
                    || !ImagenMapper.esUrlPublicable(imagen.getUrl())) {
                continue;
            }

            /* putIfAbsent: conserva la primera (menor orden) por actividad. */
            urlPorActividad.putIfAbsent(imagen.getActividad().getId(), imagen.getUrl());
        }

        for (ActividadDTO actividad : actividades) {
            actividad.setImagenPrincipalUrl(urlPorActividad.get(actividad.getId()));
        }
    }
}
