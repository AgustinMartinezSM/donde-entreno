package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.HorarioActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.HorarioActividad;

/**
 * Mapper de HorarioActividad.
 *
 * Convierte una entidad HorarioActividad en un DTO público
 * preparado para devolver por la API.
 */
public class HorarioActividadMapper {

    /**
     * Convierte HorarioActividad a HorarioActividadDTO.
     *
     * @param horario entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static HorarioActividadDTO toDTO(HorarioActividad horario) {
        if (horario == null) {
            return null;
        }

        Actividad actividad = horario.getActividad();

        Long actividadId = null;
        String actividadTitulo = null;
        String actividadSlug = null;

        if (actividad != null) {
            actividadId = actividad.getId();
            actividadTitulo = actividad.getTitulo();
            actividadSlug = actividad.getSlug();
        }

        return new HorarioActividadDTO(
                horario.getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getObservacion(),
                actividadId,
                actividadTitulo,
                actividadSlug
        );
    }
}
