package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.ImagenRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    private final ImagenRepository imagenRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public ImagenService(ImagenRepository imagenRepository) {
        this.imagenRepository = imagenRepository;
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

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
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

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
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

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
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

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
    }
}
