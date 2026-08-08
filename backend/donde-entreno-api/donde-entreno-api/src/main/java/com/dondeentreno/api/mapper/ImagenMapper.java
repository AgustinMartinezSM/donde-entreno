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
     * Indica si una URL de imagen es servible por un cliente.
     *
     * Desde el bloque de imágenes con moderación las imágenes viven en
     * Supabase Storage y se guardan con su URL absoluta. Las rutas
     * relativas ("/uploads/...") son legado de cuando los archivos se
     * guardaban en disco: la API no expone recursos estáticos y el
     * contenedor es efímero, así que no resuelven en ningún entorno.
     * Publicarlas equivale a publicar un link roto, por eso se tratan
     * como ausencia de imagen.
     *
     * @param url URL almacenada en la fila de imagen.
     * @return true si es absoluta http(s).
     */
    public static boolean esUrlPublicable(String url) {
        if (url == null) {
            return false;
        }

        String urlLimpia = url.trim();

        return urlLimpia.startsWith("https://") || urlLimpia.startsWith("http://");
    }

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
