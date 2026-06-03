package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.entity.CategoriaDeportiva;
import com.dondeentreno.api.entity.Deporte;

/**
 * Mapper de Deporte.
 *
 * Convierte una entidad Deporte en un DTO preparado
 * para devolver por la API.
 */
public class DeporteMapper {

    /**
     * Convierte una entidad Deporte en DeporteDTO.
     *
     * También incluye información básica de su categoría deportiva.
     *
     * @param deporte entidad obtenida desde la base de datos.
     * @return DTO listo para devolver al frontend.
     */
    public static DeporteDTO toDTO(Deporte deporte) {
        if (deporte == null) {
            return null;
        }

        CategoriaDeportiva categoria = deporte.getCategoriaDeportiva();

        Long categoriaId = null;
        String categoriaNombre = null;
        String categoriaSlug = null;

        if (categoria != null) {
            categoriaId = categoria.getId();
            categoriaNombre = categoria.getNombre();
            categoriaSlug = categoria.getSlug();
        }

        return new DeporteDTO(
                deporte.getId(),
                deporte.getNombre(),
                deporte.getSlug(),
                deporte.getDescripcion(),
                deporte.getIconoUrl(),
                deporte.getOrden(),
                categoriaId,
                categoriaNombre,
                categoriaSlug
        );
    }
}