package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.PublicadorActividadDetalleDTO;
import com.dondeentreno.api.dto.PublicadorActividadHorarioDTO;
import com.dondeentreno.api.dto.PublicadorActividadImagenDTO;
import com.dondeentreno.api.dto.PublicadorActividadResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.CategoriaDeportiva;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.Ubicacion;

import java.util.List;

/**
 * Mapper para actividades visibles en el panel del publicador.
 */
public class PublicadorActividadMapper {

    private static final String TIPO_IMAGEN_PRINCIPAL = "PRINCIPAL";

    private PublicadorActividadMapper() {
    }

    public static PublicadorActividadResumenDTO toResumenDTO(
            Actividad actividad,
            String imagenPrincipalUrl
    ) {
        if (actividad == null) {
            return null;
        }

        DatosActividad datos = extraerDatos(actividad);

        return new PublicadorActividadResumenDTO(
                actividad.getId(),
                actividad.getTitulo(),
                actividad.getSlug(),
                datos.deporteNombre(),
                datos.deporteSlug(),
                datos.categoriaDeportivaNombre(),
                datos.ciudadNombre(),
                datos.ciudadSlug(),
                datos.barrioNombre(),
                actividad.getEstadoPublicacion(),
                actividad.getActiva(),
                actividad.getModalidad(),
                actividad.getNivel(),
                actividad.getEdadMinima(),
                actividad.getEdadMaxima(),
                actividad.getPrecioReferencia(),
                actividad.getMostrarPrecio(),
                imagenPrincipalUrl,
                actividad.getCreatedAt(),
                actividad.getSlug()
        );
    }

    public static PublicadorActividadDetalleDTO toDetalleDTO(
            Actividad actividad,
            List<HorarioActividad> horarios,
            List<Imagen> imagenes,
            SolicitudPublicacion solicitudOrigen
    ) {
        if (actividad == null) {
            return null;
        }

        DatosActividad datos = extraerDatos(actividad);
        PerfilPublicador perfil = actividad.getPerfilPublicador();
        Ubicacion ubicacion = actividad.getUbicacion();
        List<PublicadorActividadHorarioDTO> horariosDTO = horarios == null
                ? List.of()
                : horarios.stream().map(PublicadorActividadMapper::toHorarioDTO).toList();
        List<PublicadorActividadImagenDTO> imagenesDTO = imagenes == null
                ? List.of()
                : imagenes.stream().map(PublicadorActividadMapper::toImagenDTO).toList();

        return new PublicadorActividadDetalleDTO(
                actividad.getId(),
                actividad.getTitulo(),
                actividad.getSlug(),
                datos.deporteNombre(),
                datos.deporteSlug(),
                datos.categoriaDeportivaNombre(),
                datos.ciudadNombre(),
                datos.ciudadSlug(),
                datos.barrioNombre(),
                actividad.getEstadoPublicacion(),
                actividad.getActiva(),
                actividad.getModalidad(),
                actividad.getNivel(),
                actividad.getEdadMinima(),
                actividad.getEdadMaxima(),
                actividad.getPrecioReferencia(),
                actividad.getMostrarPrecio(),
                resolverImagenPrincipalUrl(imagenes),
                actividad.getCreatedAt(),
                actividad.getSlug(),
                actividad.getDescripcion(),
                actividad.getEnfoque(),
                actividad.getRequiereInscripcion(),
                actividad.getCuposLimitados(),
                ubicacion != null ? ubicacion.getNombre() : null,
                ubicacion != null ? ubicacion.getDireccion() : null,
                ubicacion != null ? ubicacion.getReferencia() : null,
                actividad.getWhatsappContacto(),
                actividad.getInstagramContacto(),
                actividad.getEmailContacto(),
                perfil != null ? perfil.getId() : null,
                perfil != null ? perfil.getNombre() : null,
                perfil != null ? perfil.getTipoPublicador() : null,
                solicitudOrigen != null ? solicitudOrigen.getId() : null,
                solicitudOrigen != null ? solicitudOrigen.getCodigoSeguimiento() : null,
                horariosDTO,
                imagenesDTO
        );
    }

    public static PublicadorActividadHorarioDTO toHorarioDTO(HorarioActividad horario) {
        if (horario == null) {
            return null;
        }

        return new PublicadorActividadHorarioDTO(
                horario.getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getObservacion()
        );
    }

    public static PublicadorActividadImagenDTO toImagenDTO(Imagen imagen) {
        if (imagen == null) {
            return null;
        }

        return new PublicadorActividadImagenDTO(
                imagen.getId(),
                imagen.getUrl(),
                imagen.getTipoImagen(),
                imagen.getTitulo(),
                imagen.getDescripcion(),
                imagen.getOrden()
        );
    }

    public static String resolverImagenPrincipalUrl(List<Imagen> imagenes) {
        if (imagenes == null || imagenes.isEmpty()) {
            return null;
        }

        return imagenes.stream()
                .filter(imagen -> TIPO_IMAGEN_PRINCIPAL.equalsIgnoreCase(imagen.getTipoImagen()))
                .findFirst()
                .or(() -> imagenes.stream().findFirst())
                .map(Imagen::getUrl)
                .orElse(null);
    }

    private static DatosActividad extraerDatos(Actividad actividad) {
        Deporte deporte = actividad.getDeporte();
        CategoriaDeportiva categoria = deporte == null ? null : deporte.getCategoriaDeportiva();
        Ubicacion ubicacion = actividad.getUbicacion();
        Ciudad ciudad = ubicacion == null ? null : ubicacion.getCiudad();
        Barrio barrio = ubicacion == null ? null : ubicacion.getBarrio();

        return new DatosActividad(
                deporte != null ? deporte.getNombre() : null,
                deporte != null ? deporte.getSlug() : null,
                categoria != null ? categoria.getNombre() : null,
                ciudad != null ? ciudad.getNombre() : null,
                ciudad != null ? ciudad.getSlug() : null,
                barrio != null ? barrio.getNombre() : null
        );
    }

    private record DatosActividad(
            String deporteNombre,
            String deporteSlug,
            String categoriaDeportivaNombre,
            String ciudadNombre,
            String ciudadSlug,
            String barrioNombre
    ) {
    }
}
