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

        /* Campos nuevos (script 24). */
        if (solicitud.getDeporte() != null) {
            cambios.add(new CampoCambioDTO(
                    "deporte",
                    actividad.getDeporte() != null ? actividad.getDeporte().getNombre() : null,
                    solicitud.getDeporte().getNombre()
            ));
        }
        agregarSiPropuesto(
                cambios,
                "edadMinima",
                formatearEntero(actividad.getEdadMinima()),
                formatearEntero(solicitud.getEdadMinima())
        );
        agregarSiPropuesto(
                cambios,
                "edadMaxima",
                formatearEntero(actividad.getEdadMaxima()),
                formatearEntero(solicitud.getEdadMaxima())
        );
        agregarSiPropuesto(cambios, "enfoque", actividad.getEnfoque(), solicitud.getEnfoque());

        if (solicitud.getUbicacionDireccion() != null) {
            cambios.add(new CampoCambioDTO(
                    "ubicacion",
                    formatearUbicacionActual(actividad),
                    formatearUbicacionPropuesta(solicitud)
            ));
        }

        if (Boolean.TRUE.equals(solicitud.getCambiaHorarios())) {
            cambios.add(new CampoCambioDTO(
                    "horarios",
                    "Horarios vigentes de la actividad",
                    formatearHorariosPropuestos(solicitud)
            ));
        }

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
        if (solicitud.getDeporte() != null) {
            campos.add("deporte");
        }
        if (solicitud.getEdadMinima() != null) {
            campos.add("edadMinima");
        }
        if (solicitud.getEdadMaxima() != null) {
            campos.add("edadMaxima");
        }
        if (solicitud.getEnfoque() != null) {
            campos.add("enfoque");
        }
        if (solicitud.getUbicacionDireccion() != null) {
            campos.add("ubicacion");
        }
        if (Boolean.TRUE.equals(solicitud.getCambiaHorarios())) {
            campos.add("horarios");
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
        /*
          Campos planos nuevos (script 24). Ubicacion y horarios NO se
          aplican aca: necesitan repos y reglas propias (exclusiva vs
          compartida, reemplazo del conjunto) — viven en el admin
          service, dentro de la misma transaccion de aprobacion.
        */
        if (solicitud.getDeporte() != null) {
            actividad.setDeporte(solicitud.getDeporte());
        }
        if (solicitud.getEdadMinima() != null) {
            actividad.setEdadMinima(solicitud.getEdadMinima());
        }
        if (solicitud.getEdadMaxima() != null) {
            actividad.setEdadMaxima(solicitud.getEdadMaxima());
        }
        if (solicitud.getEnfoque() != null) {
            actividad.setEnfoque(solicitud.getEnfoque());
        }
    }

    private static String formatearEntero(Integer valor) {
        return valor != null ? String.valueOf(valor) : null;
    }

    private static String formatearUbicacionActual(Actividad actividad) {
        var ubicacion = actividad.getUbicacion();

        if (ubicacion == null) {
            return null;
        }

        String barrio = ubicacion.getBarrio() != null
                ? ubicacion.getBarrio().getNombre()
                : null;

        return formatearUbicacion(
                ubicacion.getNombre(),
                ubicacion.getDireccion(),
                ubicacion.getReferencia(),
                barrio
        );
    }

    private static String formatearUbicacionPropuesta(SolicitudCambioActividad solicitud) {
        String barrio = solicitud.getUbicacionBarrio() != null
                ? solicitud.getUbicacionBarrio().getNombre()
                : null;

        return formatearUbicacion(
                solicitud.getUbicacionNombre(),
                solicitud.getUbicacionDireccion(),
                solicitud.getUbicacionReferencia(),
                barrio
        );
    }

    private static String formatearUbicacion(
            String nombre,
            String direccion,
            String referencia,
            String barrio
    ) {
        List<String> partes = new ArrayList<>();

        if (nombre != null) {
            partes.add(nombre);
        }
        if (direccion != null) {
            partes.add(direccion);
        }
        if (barrio != null) {
            partes.add("Barrio " + barrio);
        }
        if (referencia != null) {
            partes.add("(" + referencia + ")");
        }

        return String.join(", ", partes);
    }

    private static String formatearHorariosPropuestos(SolicitudCambioActividad solicitud) {
        List<String> lineas = new ArrayList<>();

        for (var horario : solicitud.getHorarios()) {
            String linea = horario.getDiaSemana() + " "
                    + horario.getHoraInicio() + " a " + horario.getHoraFin();

            if (horario.getObservacion() != null) {
                linea += " (" + horario.getObservacion() + ")";
            }

            lineas.add(linea);
        }

        return String.join(" | ", lineas);
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
