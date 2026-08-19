package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Una actividad guardada por un usuario (script 20, sync de favoritos).
 *
 * (usuario, actividad) es unico: guardar es idempotente, y dejar de
 * guardar borra la fila — igual que seguimientos.
 *
 * FKs planas (y no @ManyToOne) a proposito: este registro nunca navega a
 * sus entidades — el listado se resuelve en dos pasos (ids del favorito,
 * actividades con su propio query filtrado y EntityGraph), el mismo
 * patron del feed de seguidos.
 */
@Entity
@Table(name = "favorito_actividad")
public class FavoritoActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public FavoritoActividad() {
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
