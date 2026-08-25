package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Lo que el publicador le avisa al grupo de una actividad (script 38).
 *
 * Mismo molde que {@link Novedad}, con una diferencia de alcance que
 * es la razon de ser del grupo: la novedad va a TODOS sus seguidores,
 * el aviso va solo a quienes van a esa actividad.
 */
@Entity
@Table(name = "aviso_grupo")
public class AvisoGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "texto", nullable = false, length = 1000)
    private String texto;

    /** Una foto YA publicada del publicador. Opcional. */
    @Column(name = "imagen_id")
    private Long imagenId;

    /** VISIBLE | OCULTO_POR_ADMIN | ELIMINADO_POR_PUBLICADOR. */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public AvisoGrupo() {
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

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
