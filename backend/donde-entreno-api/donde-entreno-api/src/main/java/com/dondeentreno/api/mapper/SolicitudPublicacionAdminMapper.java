package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminHorarioDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminRevisorDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;

import java.util.List;

/**
 * Mapper de solicitudes de publicacion para el panel admin.
 */
public class SolicitudPublicacionAdminMapper {

    public static SolicitudPublicacionAdminResumenDTO toResumenDTO(SolicitudPublicacion solicitud) {
        if (solicitud == null) {
            return null;
        }

        Deporte deporte = solicitud.getDeporte();
        Ciudad ciudad = solicitud.getCiudad();
        Barrio barrio = solicitud.getBarrio();

        return new SolicitudPublicacionAdminResumenDTO(
                solicitud.getId(),
                solicitud.getCodigoSeguimiento(),
                solicitud.getEstado(),
                solicitud.getOrigen(),
                solicitud.getTipoPublicador(),
                solicitud.getNombrePublicador(),
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
                solicitud.getEmail(),
                solicitud.getWhatsapp(),
                solicitud.getCreatedAt(),
                solicitud.getUpdatedAt(),
                solicitud.getRevisionIniciadaAt(),
                solicitud.getRevisionFinalizadaAt()
        );
    }

    public static SolicitudPublicacionAdminDetalleDTO toDetalleDTO(
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

        List<SolicitudPublicacionAdminHorarioDTO> horariosDTO = horarios == null
                ? List.of()
                : horarios.stream()
                        .map(SolicitudPublicacionAdminMapper::toHorarioDTO)
                        .toList();

        return new SolicitudPublicacionAdminDetalleDTO(
                solicitud.getId(),
                solicitud.getCodigoSeguimiento(),
                solicitud.getEstado(),
                solicitud.getOrigen(),
                solicitud.getTipoPublicador(),
                solicitud.getNombrePublicador(),
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
                solicitud.getEmail(),
                solicitud.getWhatsapp(),
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
                solicitud.getInstagram(),
                solicitud.getObservacionesSolicitante(),
                solicitud.getMotivoRechazo(),
                solicitud.getObservacionesRevision(),
                toRevisorDTO(solicitud.getRevisadoPorUsuario()),
                actividadGenerada != null ? actividadGenerada.getId() : null,
                horariosDTO
        );
    }

    public static SolicitudPublicacionAdminHorarioDTO toHorarioDTO(SolicitudPublicacionHorario horario) {
        if (horario == null) {
            return null;
        }

        return new SolicitudPublicacionAdminHorarioDTO(
                horario.getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getObservacion()
        );
    }

    public static SolicitudPublicacionAdminRevisorDTO toRevisorDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        Rol rol = usuario.getRol();

        return new SolicitudPublicacionAdminRevisorDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                rol != null ? rol.getNombre() : null
        );
    }
}
