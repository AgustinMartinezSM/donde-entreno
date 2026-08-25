package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Novedad del canal de un publicador (script 34, Fase 8).
 *
 * Lo que quiere contar sin tener que crear o editar una actividad:
 * "cambiamos el horario del sábado", "quedan 3 lugares". Publica
 * directo y se modera por reportes, como todo lo social desde la
 * Fase 4.
 *
 * FKs planas (patrón de las entidades sociales): el service resuelve
 * lo que necesita con queries batch.
 */
@Entity
@Table(name = "novedad")
public class Novedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_publicador_id", nullable = false)
    private Long perfilPublicadorId;

    @Column(name = "texto", nullable = false, length = 1000)
    private String texto;

    /** Una foto YA publicada del publicador. Opcional. */
    @Column(name = "imagen_id")
    private Long imagenId;

    /** VISIBLE | OCULTA_POR_ADMIN | ELIMINADA_POR_PUBLICADOR. */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Novedad() {
    }

    public Long getId() {
        return id;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
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
