package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Interacción anónima sobre una actividad o un perfil de publicador
 * (script 28, ampliada por el 31). SIN usuario a propósito:
 * privacidad primero — se cuentan eventos, nunca quién.
 *
 * El evento cuelga de UNO de los dos: `actividadId` (detalle) o
 * `perfilPublicadorId` (perfil). Que venga al menos uno lo valida el
 * service; en la base las dos columnas son nullable.
 */
@Entity
@Table(name = "evento_interaccion")
public class EventoInteraccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actividad_id")
    private Long actividadId;

    /** Fase 5 (script 31): el click de contacto desde el perfil. */
    @Column(name = "perfil_publicador_id")
    private Long perfilPublicadorId;

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

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
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
