package com.dondeentreno.api.mapper;

import com.dondeentreno.api.dto.CampoCambioDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.SolicitudCambioActividad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers estaticos del flujo de solicitudes de cambio:
 * comparacion campo a campo, listado de campos propuestos y
 * aplicacion de los cambios aprobados sobre la actividad.
 */
public final class SolicitudCambioActividadMapper {

    private SolicitudCambioActividadMapper() {
    }

    /**
     * Arma la comparacion valor actual vs valor propuesto,
     * solo para los campos con cambio propuesto.
     * Debe llamarse ANTES de aplicar los cambios.
     */
    public static List<CampoCambioDTO> construirCambios(
            SolicitudCambioActividad solicitud,
            Actividad actividad
    ) {
        List<CampoCambioDTO> cambios = new ArrayList<>();

        agregarSiPropuesto(cambios, "titulo", actividad.getTitulo(), solicitud.getTitulo());
        agregarSiPropuesto(cambios, "descripcion", actividad.getDescripcion(), solicitud.getDescripcion());
        agregarSiPropuesto(
                cambios,
                "precioReferencia",
                formatearPrecio(actividad.getPrecioReferencia()),
                formatearPrecio(solicitud.getPrecioReferencia())
        );
        agregarSiPropuesto(
                cambios,
                "mostrarPrecio",
                formatearBooleano(actividad.getMostrarPrecio()),
                formatearBooleano(solicitud.getMostrarPrecio())
        );
        agregarSiPropuesto(cambios, "whatsappContacto", actividad.getWhatsappContacto(), solicitud.getWhatsappContacto());
        agregarSiPropuesto(cambios, "instagramContacto", actividad.getInstagramContacto(), solicitud.getInstagramContacto());
        agregarSiPropuesto(cambios, "emailContacto", actividad.getEmailContacto(), solicitud.getEmailContacto());
        agregarSiPropuesto(cambios, "nivel", actividad.getNivel(), solicitud.getNivel());
        agregarSiPropuesto(cambios, "modalidad", actividad.getModalidad(), solicitud.getModalidad());

        return cambios;
    }

    /**
     * Nombres de los campos con cambio propuesto (para listados).
     */
    public static List<String> listarCamposPropuestos(SolicitudCambioActividad solicitud) {
        List<String> campos = new ArrayList<>();

        if (solicitud.getTitulo() != null) {
            campos.add("titulo");
        }
        if (solicitud.getDescripcion() != null) {
            campos.add("descripcion");
        }
        if (solicitud.getPrecioReferencia() != null) {
            campos.add("precioReferencia");
        }
        if (solicitud.getMostrarPrecio() != null) {
            campos.add("mostrarPrecio");
        }
        if (solicitud.getWhatsappContacto() != null) {
            campos.add("whatsappContacto");
        }
        if (solicitud.getInstagramContacto() != null) {
            campos.add("instagramContacto");
        }
        if (solicitud.getEmailContacto() != null) {
            campos.add("emailContacto");
        }
        if (solicitud.getNivel() != null) {
            campos.add("nivel");
        }
        if (solicitud.getModalidad() != null) {
            campos.add("modalidad");
        }

        return campos;
    }

    /**
     * Aplica sobre la actividad SOLO los campos propuestos (no nulos).
     * Se usa en la aprobacion, dentro de la transaccion.
     */
    public static void aplicarCambios(
            SolicitudCambioActividad solicitud,
            Actividad actividad
    ) {
        if (solicitud.getTitulo() != null) {
            actividad.setTitulo(solicitud.getTitulo());
        }
        if (solicitud.getDescripcion() != null) {
            actividad.setDescripcion(solicitud.getDescripcion());
        }
        if (solicitud.getPrecioReferencia() != null) {
            actividad.setPrecioReferencia(solicitud.getPrecioReferencia());
        }
        if (solicitud.getMostrarPrecio() != null) {
            actividad.setMostrarPrecio(solicitud.getMostrarPrecio());
        }
        if (solicitud.getWhatsappContacto() != null) {
            actividad.setWhatsappContacto(solicitud.getWhatsappContacto());
        }
        if (solicitud.getInstagramContacto() != null) {
            actividad.setInstagramContacto(solicitud.getInstagramContacto());
        }
        if (solicitud.getEmailContacto() != null) {
            actividad.setEmailContacto(solicitud.getEmailContacto());
        }
        if (solicitud.getNivel() != null) {
            actividad.setNivel(solicitud.getNivel());
        }
        if (solicitud.getModalidad() != null) {
            actividad.setModalidad(solicitud.getModalidad());
        }
    }

    private static void agregarSiPropuesto(
            List<CampoCambioDTO> cambios,
            String campo,
            String valorActual,
            String valorPropuesto
    ) {
        if (valorPropuesto != null) {
            cambios.add(new CampoCambioDTO(campo, valorActual, valorPropuesto));
        }
    }

    private static String formatearPrecio(BigDecimal precio) {
        return precio != null ? precio.toPlainString() : null;
    }

    private static String formatearBooleano(Boolean valor) {
        if (valor == null) {
            return null;
        }
        return valor ? "Si" : "No";
    }
}
