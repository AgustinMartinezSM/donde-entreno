package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * DTO de entrada para horarios de una solicitud publica de publicacion.
 */
public class SolicitudPublicacionHorarioRequestDTO {

    @NotBlank(message = "El dia de la semana es obligatorio.")
    @Size(max = 20, message = "El dia de la semana no puede superar los 20 caracteres.")
    @Pattern(
            regexp = "LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO",
            message = "El dia de la semana informado no es valido."
    )
    private String diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria.")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de finalizacion es obligatoria.")
    private LocalTime horaFin;

    @Size(max = 255, message = "La observacion del horario no puede superar los 255 caracteres.")
    private String observacion;

    public SolicitudPublicacionHorarioRequestDTO() {
    }

    public SolicitudPublicacionHorarioRequestDTO(
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String observacion
    ) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.observacion = observacion;
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
