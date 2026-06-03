package com.dondeentreno.api.dto;

import java.time.LocalTime;

/**
 * DTO de HorarioActividad.
 *
 * Representa los datos públicos de un horario de actividad
 * que vamos a devolver desde la API hacia el frontend.
 */
public class HorarioActividadDTO {

    private Long id;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String observacion;

    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;

    public HorarioActividadDTO() {
    }

    public HorarioActividadDTO(
            Long id,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String observacion,
            Long actividadId,
            String actividadTitulo,
            String actividadSlug
    ) {
        this.id = id;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.observacion = observacion;
        this.actividadId = actividadId;
        this.actividadTitulo = actividadTitulo;
        this.actividadSlug = actividadSlug;
    }

    public Long getId() {
        return id;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getObservacion() {
        return observacion;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public String getActividadTitulo() {
        return actividadTitulo;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public void setActividadTitulo(String actividadTitulo) {
        this.actividadTitulo = actividadTitulo;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }
}
