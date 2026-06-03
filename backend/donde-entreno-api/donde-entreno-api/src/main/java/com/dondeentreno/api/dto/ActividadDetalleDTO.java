package com.dondeentreno.api.dto;

import java.util.List;

/**
 * DTO de detalle completo de actividad.
 *
 * Agrupa en una sola respuesta:
 * - Datos principales de la actividad
 * - Horarios activos
 * - Imágenes activas
 *
 * Esto es útil para la pantalla de detalle del frontend.
 */
public class ActividadDetalleDTO {

    private ActividadDTO actividad;
    private List<HorarioActividadDTO> horarios;
    private List<ImagenDTO> imagenes;

    public ActividadDetalleDTO() {
    }

    public ActividadDetalleDTO(
            ActividadDTO actividad,
            List<HorarioActividadDTO> horarios,
            List<ImagenDTO> imagenes
    ) {
        this.actividad = actividad;
        this.horarios = horarios;
        this.imagenes = imagenes;
    }

    public ActividadDTO getActividad() {
        return actividad;
    }

    public List<HorarioActividadDTO> getHorarios() {
        return horarios;
    }

    public List<ImagenDTO> getImagenes() {
        return imagenes;
    }

    public void setActividad(ActividadDTO actividad) {
        this.actividad = actividad;
    }

    public void setHorarios(List<HorarioActividadDTO> horarios) {
        this.horarios = horarios;
    }

    public void setImagenes(List<ImagenDTO> imagenes) {
        this.imagenes = imagenes;
    }
}
