package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Interacción anónima sobre una actividad (script 28, Fase 2 social).
 * SIN usuario a propósito: privacidad primero — se cuentan eventos,
 * nunca quién.
 */
@Entity
@Table(name = "evento_interaccion")
public class EventoInteraccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public EventoInteraccion() {
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
