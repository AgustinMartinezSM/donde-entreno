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
 * Entidad que representa la tabla horario_actividad de PostgreSQL.
 *
 * Guarda los días y horarios en los que se realiza una actividad.
 *
 * Ejemplos:
 * - Lunes 18:00 a 19:30
 * - Miércoles 18:00 a 19:30
 * - Viernes 19:00 a 20:30
 *
 * Cada horario pertenece a una actividad.
 */
@Entity
@Table(name = "horario_actividad")
public class HorarioActividad {

    /**
     * Identificador único del horario.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Actividad a la que pertenece este horario.
     *
     * En la tabla horario_actividad existe la columna:
     * actividad_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    /**
     * Día de la semana.
     *
     * Valores permitidos en la base:
     * LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO.
     *
     * Por ahora lo manejamos como String para avanzar simple.
     * Más adelante podemos convertirlo a enum.
     */
    @Column(name = "dia_semana", nullable = false, length = 20)
    private String diaSemana;

    /**
     * Hora de inicio de la clase o actividad.
     *
     * En PostgreSQL corresponde a:
     * TIME NOT NULL
     */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /**
     * Hora de finalización de la clase o actividad.
     *
     * En PostgreSQL corresponde a:
     * TIME NOT NULL
     */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /**
     * Observación opcional del horario.
     *
     * Ejemplo:
     * "Consultar disponibilidad"
     * "Clase para principiantes"
     */
    @Column(name = "observacion", length = 255)
    private String observacion;

    /**
     * Indica si este horario está activo.
     *
     * En la tabla se llama activo.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    /**
     * Fecha de creación del registro.
     *
     * En PostgreSQL corresponde a TIMESTAMPTZ.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización.
     *
     * En PostgreSQL corresponde a TIMESTAMPTZ.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Constructor vacío obligatorio para JPA.
     */
    public HorarioActividad() {
    }

    public Long getId() {
        return id;
    }

    public Actividad getActividad() {
        return actividad;
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

    public Boolean getActivo() {
        return activo;
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

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
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

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
