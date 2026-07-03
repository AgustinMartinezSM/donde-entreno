package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.entity.Ciudad;

/**
 * Mapper de Ciudad.
 *
 * Convierte una entidad Ciudad en un DTO preparado
 * para devolver por la API.
 */
public class CiudadMapper {

    /**
     * Convierte una entidad Ciudad en CiudadDTO.
     *
     * @param ciudad entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static CiudadDTO toDTO(Ciudad ciudad) {
        if (ciudad == null) {
            return null;
        }

        return new CiudadDTO(
                ciudad.getId(),
                ciudad.getNombre(),
                ciudad.getProvincia(),
                ciudad.getPais(),
                ciudad.getSlug(),
                ciudad.getOrden(),
                ciudad.getActiva()
        );
    }
}
