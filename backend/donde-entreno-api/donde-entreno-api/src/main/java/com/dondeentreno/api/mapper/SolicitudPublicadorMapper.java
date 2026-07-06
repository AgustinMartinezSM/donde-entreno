package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.SolicitudPublicadorDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorHorarioDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;

import java.util.List;

/**
 * Mapper de solicitudes de publicacion para el panel publicador.
 */
public class SolicitudPublicadorMapper {

    private SolicitudPublicadorMapper() {
    }

    public static SolicitudPublicadorResumenDTO toResumenDTO(SolicitudPublicacion solicitud) {
        if (solicitud == null) {
            return null;
        }

        Deporte deporte = solicitud.getDeporte();
        Ciudad ciudad = solicitud.getCiudad();
        Barrio barrio = solicitud.getBarrio();

        return new SolicitudPublicadorResumenDTO(
                solicitud.getId(),
                solicitud.getCodigoSeguimiento(),
                solicitud.getEstado(),
                solicitud.getNombreActividad(),
                deporte != null ? deporte.getId() : null,
                deporte != null ? deporte.getNombre() : null,
                solicitud.getDeporteOtro(),
                ciudad != null ? ciudad.getId() : null,
                ciudad != null ? ciudad.getNombre() : null,
                solicitud.getCiudadOtra(),
                barrio != null ? barrio.getId() : null,
                barrio != null ? barrio.getNombre() : null,
                solicitud.getBarrioOtro(),
                solicitud.getCreatedAt(),
                solicitud.getUpdatedAt(),
                solicitud.getRevisionIniciadaAt(),
                solicitud.getRevisionFinalizadaAt(),
                solicitud.getMotivoRechazo()
        );
    }

    public static SolicitudPublicadorDetalleDTO toDetalleDTO(
            SolicitudPublicacion solicitud,
            List<SolicitudPublicacionHorario> horarios
    ) {
        if (solicitud == null) {
            return null;
        }

        Deporte deporte = solicitud.getDeporte();
        Ciudad ciudad = solicitud.getCiudad();
        Barrio barrio = solicitud.getBarrio();
        Actividad actividadGenerada = solicitud.getActividadGenerada();

        List<SolicitudPublicadorHorarioDTO> horariosDTO = horarios == null
                ? List.of()
                : horarios.stream()
                        .map(SolicitudPublicadorMapper::toHorarioDTO)
                        .toList();

        return new SolicitudPublicadorDetalleDTO(
                solicitud.getId(),
                solicitud.getCodigoSeguimiento(),
                solicitud.getEstado(),
                solicitud.getNombreActividad(),
                deporte != null ? deporte.getId() : null,
                deporte != null ? deporte.getNombre() : null,
                solicitud.getDeporteOtro(),
                ciudad != null ? ciudad.getId() : null,
                ciudad != null ? ciudad.getNombre() : null,
                solicitud.getCiudadOtra(),
                barrio != null ? barrio.getId() : null,
                barrio != null ? barrio.getNombre() : null,
                solicitud.getBarrioOtro(),
                solicitud.getCreatedAt(),
                solicitud.getUpdatedAt(),
                solicitud.getRevisionIniciadaAt(),
                solicitud.getRevisionFinalizadaAt(),
                solicitud.getDescripcion(),
                solicitud.getNivel(),
                solicitud.getEnfoque(),
                solicitud.getModalidad(),
                solicitud.getEdadMinima(),
                solicitud.getEdadMaxima(),
                solicitud.getPrecioReferencia(),
                solicitud.getMostrarPrecio(),
                solicitud.getNombreLugar(),
                solicitud.getDireccion(),
                solicitud.getReferenciaUbicacion(),
                solicitud.getWhatsapp(),
                solicitud.getInstagram(),
                solicitud.getEmail(),
                solicitud.getObservacionesSolicitante(),
                solicitud.getMotivoRechazo(),
                actividadGenerada != null ? actividadGenerada.getId() : null,
                horariosDTO
        );
    }

    public static SolicitudPublicadorHorarioDTO toHorarioDTO(SolicitudPublicacionHorario horario) {
        if (horario == null) {
            return null;
        }

        return new SolicitudPublicadorHorarioDTO(
                horario.getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getObservacion()
        );
    }
}
