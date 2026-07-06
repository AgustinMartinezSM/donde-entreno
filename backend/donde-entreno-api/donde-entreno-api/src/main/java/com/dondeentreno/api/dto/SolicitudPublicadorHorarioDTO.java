package com.dondeentreno.api.dto;

import java.time.LocalTime;

/**
 * Horario visible para el panel publicador.
 */
public class SolicitudPublicadorHorarioDTO {

    private Long id;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String observacion;

    public SolicitudPublicadorHorarioDTO() {
    }

    public SolicitudPublicadorHorarioDTO(
            Long id,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String observacion
    ) {
        this.id = id;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.observacion = observacion;
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
}
