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
     * Obtiene imágenes activas de una actividad por slug.
     *
     * @param actividadSlug slug de la actividad.
     * @return lista de imágenes activas en formato DTO.
     */
    public List<ImagenDTO> obtenerImagenesPorActividadSlug(String actividadSlug) {
        List<Imagen> imagenes =
                imagenRepository.findByActivaTrueAndActividad_SlugOrderByOrdenAsc(actividadSlug);

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene imágenes activas de una actividad por slug y tipo.
     *
     * @param actividadSlug slug de la actividad.
     * @param tipoImagen tipo de imagen.
     * @return lista de imágenes activas filtradas por tipo.
     */
    public List<ImagenDTO> obtenerImagenesPorActividadSlugYTipo(
            String actividadSlug,
            String tipoImagen
    ) {
        List<Imagen> imagenes =
                imagenRepository.findByActivaTrueAndActividad_SlugAndTipoImagenOrderByOrdenAsc(
                        actividadSlug,
                        tipoImagen
                );

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene imágenes activas de un perfil publicador por ID.
     *
     * @param perfilPublicadorId ID del perfil publicador.
     * @return lista de imágenes activas en formato DTO.
     */
    public List<ImagenDTO> obtenerImagenesPorPerfilPublicador(Long perfilPublicadorId) {
        List<Imagen> imagenes =
                imagenRepository.findByActivaTrueAndPerfilPublicador_IdOrderByOrdenAsc(perfilPublicadorId);

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene imágenes activas de un perfil publicador por ID y tipo.
     *
     * @param perfilPublicadorId ID del perfil publicador.
     * @param tipoImagen tipo de imagen.
     * @return lista de imágenes activas filtradas por tipo.
     */
    public List<ImagenDTO> obtenerImagenesPorPerfilPublicadorYTipo(
            Long perfilPublicadorId,
            String tipoImagen
    ) {
        List<Imagen> imagenes =
                imagenRepository.findByActivaTrueAndPerfilPublicador_IdAndTipoImagenOrderByOrdenAsc(
                        perfilPublicadorId,
                        tipoImagen
                );

        return imagenes.stream()
                .map(ImagenMapper::toDTO)
                .toList();
    }
}
