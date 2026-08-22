package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Check-in "Entrené acá" de un usuario sobre una actividad (script 26).
 *
 * Sin UNIQUE a propósito: se entrena muchas veces. La regla "1 por
 * actividad por día" la valida el service contra la base. FKs planas
 * como en MeGustaImagen: este registro nunca navega a sus entidades.
 */
@Entity
@Table(name = "entrenamiento_usuario")
public class EntrenamientoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public EntrenamientoUsuario() {
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
