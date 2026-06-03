package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.CategoriaDeportiva;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Ubicacion;

/**
 * Mapper de Actividad.
 *
 * Convierte una entidad Actividad en un DTO público preparado
 * para devolver por la API.
 */
public class ActividadMapper {

    /**
     * Convierte Actividad a ActividadDTO.
     *
     * Incluye datos básicos de:
     * - Perfil publicador
     * - Deporte
     * - Categoría deportiva
     * - Ubicación
     * - Ciudad
     * - Barrio
     *
     * @param actividad entidad obtenida desde PostgreSQL.
     * @return DTO listo para devolver al frontend.
     */
    public static ActividadDTO toDTO(Actividad actividad) {
        if (actividad == null) {
            return null;
        }

        PerfilPublicador perfil = actividad.getPerfilPublicador();
        Deporte deporte = actividad.getDeporte();
        Ubicacion ubicacion = actividad.getUbicacion();

        CategoriaDeportiva categoria = null;
        Ciudad ciudad = null;
        Barrio barrio = null;

        if (deporte != null) {
            categoria = deporte.getCategoriaDeportiva();
        }

        if (ubicacion != null) {
            ciudad = ubicacion.getCiudad();
            barrio = ubicacion.getBarrio();
        }

        Long perfilId = null;
        String perfilNombre = null;
        String tipoPublicador = null;
        Boolean perfilVerificado = null;

        if (perfil != null) {
            perfilId = perfil.getId();
            perfilNombre = perfil.getNombre();
            tipoPublicador = perfil.getTipoPublicador();
            perfilVerificado = perfil.getVerificado();
        }

        Long deporteId = null;
        String deporteNombre = null;
        String deporteSlug = null;

        if (deporte != null) {
            deporteId = deporte.getId();
            deporteNombre = deporte.getNombre();
            deporteSlug = deporte.getSlug();
        }

        Long categoriaId = null;
        String categoriaNombre = null;
        String categoriaSlug = null;

        if (categoria != null) {
            categoriaId = categoria.getId();
            categoriaNombre = categoria.getNombre();
            categoriaSlug = categoria.getSlug();
        }

        Long ubicacionId = null;
        String ubicacionNombre = null;
        String direccion = null;

        if (ubicacion != null) {
            ubicacionId = ubicacion.getId();
            ubicacionNombre = ubicacion.getNombre();
            direccion = ubicacion.getDireccion();
        }

        Long ciudadId = null;
        String ciudadNombre = null;

        if (ciudad != null) {
            ciudadId = ciudad.getId();
            ciudadNombre = ciudad.getNombre();
        }

        Long barrioId = null;
        String barrioNombre = null;

        if (barrio != null) {
            barrioId = barrio.getId();
            barrioNombre = barrio.getNombre();
        }

        return new ActividadDTO(
                actividad.getId(),
                actividad.getTitulo(),
                actividad.getSlug(),
                actividad.getDescripcion(),
                actividad.getEdadMinima(),
                actividad.getEdadMaxima(),
                actividad.getNivel(),
                actividad.getEnfoque(),
                actividad.getModalidad(),
                actividad.getPrecioReferencia(),
                actividad.getMostrarPrecio(),
                actividad.getRequiereInscripcion(),
                actividad.getCuposLimitados(),
                actividad.getWhatsappContacto(),
                actividad.getInstagramContacto(),
                actividad.getEmailContacto(),
                perfilId,
                perfilNombre,
                tipoPublicador,
                perfilVerificado,
                deporteId,
                deporteNombre,
                deporteSlug,
                categoriaId,
                categoriaNombre,
                categoriaSlug,
                ubicacionId,
                ubicacionNombre,
                direccion,
                ciudadId,
                ciudadNombre,
                barrioId,
                barrioNombre
        );
    }
}
