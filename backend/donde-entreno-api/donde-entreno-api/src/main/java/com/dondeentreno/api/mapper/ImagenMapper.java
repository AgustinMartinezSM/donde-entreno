package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;

/**
 * Mapper de Imagen.
 *
 * Convierte una entidad Imagen en un DTO público
 * preparado para devolver por la API.
 */
public class ImagenMapper {

    /**
     * Convierte Imagen a ImagenDTO.
     *
     * @param imagen entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static ImagenDTO toDTO(Imagen imagen) {
        if (imagen == null) {
            return null;
        }

        Actividad actividad = imagen.getActividad();
        PerfilPublicador perfilPublicador = imagen.getPerfilPublicador();

        Long actividadId = null;
        String actividadSlug = null;

        if (actividad != null) {
            actividadId = actividad.getId();
            actividadSlug = actividad.getSlug();
        }

        Long perfilPublicadorId = null;
        String perfilPublicadorNombre = null;

        if (perfilPublicador != null) {
            perfilPublicadorId = perfilPublicador.getId();
            perfilPublicadorNombre = perfilPublicador.getNombre();
        }

        return new ImagenDTO(
                imagen.getId(),
                imagen.getUrl(),
                imagen.getTipoImagen(),
                imagen.getTitulo(),
                imagen.getDescripcion(),
                imagen.getOrden(),
                actividadId,
                actividadSlug,
                perfilPublicadorId,
                perfilPublicadorNombre
        );
    }
}
