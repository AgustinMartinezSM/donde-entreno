package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Pregunta pública sobre una actividad (script 29), patrón
 * MercadoLibre: la respuesta ÚNICA del publicador vive en esta misma
 * fila. Publica directo; se modera por reportes.
 */
@Entity
@Table(name = "pregunta_actividad")
public class PreguntaActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "pregunta", nullable = false, length = 500)
    private String pregunta;

    @Column(name = "respuesta", length = 1000)
    private String respuesta;

    @Column(name = "respondida_at")
    private OffsetDateTime respondidaAt;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "VISIBLE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public PreguntaActividad() {
    }

    public Long getId() {
        return id;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public OffsetDateTime getRespondidaAt() {
        return respondidaAt;
    }

    public void setRespondidaAt(OffsetDateTime respondidaAt) {
        this.respondidaAt = respondidaAt;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
