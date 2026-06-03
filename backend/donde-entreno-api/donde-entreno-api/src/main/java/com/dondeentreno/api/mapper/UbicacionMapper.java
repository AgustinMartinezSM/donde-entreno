package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.UbicacionDTO;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Ubicacion;

/**
 * Mapper de Ubicacion.
 *
 * Convierte una entidad Ubicacion en un DTO preparado
 * para devolver por la API.
 */
public class UbicacionMapper {

    /**
     * Convierte una entidad Ubicacion en UbicacionDTO.
     *
     * También incluye datos básicos del perfil publicador,
     * la ciudad y el barrio relacionados.
     *
     * @param ubicacion entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static UbicacionDTO toDTO(Ubicacion ubicacion) {
        if (ubicacion == null) {
            return null;
        }

        PerfilPublicador perfilPublicador = ubicacion.getPerfilPublicador();
        Ciudad ciudad = ubicacion.getCiudad();
        Barrio barrio = ubicacion.getBarrio();

        Long perfilPublicadorId = null;
        String perfilPublicadorNombre = null;

        Long ciudadId = null;
        String ciudadNombre = null;

        Long barrioId = null;
        String barrioNombre = null;

        if (perfilPublicador != null) {
            perfilPublicadorId = perfilPublicador.getId();
            perfilPublicadorNombre = perfilPublicador.getNombre();
        }

        if (ciudad != null) {
            ciudadId = ciudad.getId();
            ciudadNombre = ciudad.getNombre();
        }

        if (barrio != null) {
            barrioId = barrio.getId();
            barrioNombre = barrio.getNombre();
        }

        return new UbicacionDTO(
                ubicacion.getId(),
                ubicacion.getNombre(),
                ubicacion.getDireccion(),
                ubicacion.getReferencia(),
                ubicacion.getLatitud(),
                ubicacion.getLongitud(),
                ubicacion.getGoogleMapsUrl(),
                perfilPublicadorId,
                perfilPublicadorNombre,
                ciudadId,
                ciudadNombre,
                barrioId,
                barrioNombre
        );
    }
}
