package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Entidad que representa la tabla solicitud_publicacion_horario de PostgreSQL.
 *
 * Guarda los horarios estructurados informados para una solicitud publica.
 */
@Entity
@Table(name = "solicitud_publicacion_horario")
public class SolicitudPublicacionHorario {

    /**
     * Identificador unico del horario de solicitud.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Solicitud publica a la que pertenece este horario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_publicacion_id", nullable = false)
    private SolicitudPublicacion solicitudPublicacion;

    /**
     * Dia de la semana.
     */
    @Column(name = "dia_semana", nullable = false, length = 20)
    private String diaSemana;

    /**
     * Hora de inicio.
     */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /**
     * Hora de finalizacion.
     */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /**
     * Observacion opcional del horario.
     */
    @Column(name = "observacion", length = 255)
    private String observacion;

    /**
     * Fecha de creacion.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Fecha de actualizacion.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Constructor vacio obligatorio para JPA.
     */
    public SolicitudPublicacionHorario() {
    }

    public Long getId() {
        return id;
    }

    public SolicitudPublicacion getSolicitudPublicacion() {
        return solicitudPublicacion;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSolicitudPublicacion(SolicitudPublicacion solicitudPublicacion) {
        this.solicitudPublicacion = solicitudPublicacion;
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

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
