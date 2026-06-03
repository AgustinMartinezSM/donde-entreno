package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;

/**
 * Mapper de Barrio.
 *
 * Convierte una entidad Barrio en un DTO preparado
 * para devolver por la API.
 */
public class BarrioMapper {

    /**
     * Convierte una entidad Barrio en BarrioDTO.
     *
     * También incluye información básica de la ciudad
     * a la que pertenece el barrio.
     *
     * @param barrio entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static BarrioDTO toDTO(Barrio barrio) {
        if (barrio == null) {
            return null;
        }

        Ciudad ciudad = barrio.getCiudad();

        Long ciudadId = null;
        String ciudadNombre = null;

        if (ciudad != null) {
            ciudadId = ciudad.getId();
            ciudadNombre = ciudad.getNombre();
        }

        return new BarrioDTO(
                barrio.getId(),
                barrio.getNombre(),
                ciudadId,
                ciudadNombre
        );
    }
}