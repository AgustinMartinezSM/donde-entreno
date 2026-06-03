package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.entity.CategoriaDeportiva;

/**
 * Mapper de CategoriaDeportiva.
 *
 * Esta clase se encarga de convertir una entidad de base de datos
 * en un DTO preparado para devolver por la API.
 */
public class CategoriaDeportivaMapper {

    /**
     * Convierte una entidad CategoriaDeportiva en CategoriaDeportivaDTO.
     *
     * @param categoria Entidad obtenida desde la base de datos.
     * @return DTO listo para devolver al frontend.
     */
    public static CategoriaDeportivaDTO toDTO(CategoriaDeportiva categoria) {
        if (categoria == null) {
            return null;
        }

        return new CategoriaDeportivaDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getSlug(),
                categoria.getDescripcion(),
                categoria.getIconoUrl(),
                categoria.getOrden()
        );
    }
}