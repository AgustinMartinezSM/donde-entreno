package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Una conversación privada entre un usuario y un publicador
 * (script 36).
 *
 * La inicia SIEMPRE el usuario: el publicador no puede escribir en
 * frío, porque un primer mensaje no solicitado destruye la confianza
 * en la bandeja entera.
 *
 * FKs planas (patrón de las entidades sociales).
 */
@Entity
@Table(name = "conversacion")
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "perfil_publicador_id", nullable = false)
    private Long perfilPublicadorId;

    /** Opcional: se puede consultar por el club en general. */
    @Column(name = "actividad_id")
    private Long actividadId;

    /** ABIERTA | CERRADA_POR_USUARIO. */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /** Para ordenar la bandeja sin mirar la tabla de mensajes. */
    @Column(name = "ultimo_mensaje_at", nullable = false)
    private OffsetDateTime ultimoMensajeAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Conversacion() {
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

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getUltimoMensajeAt() {
        return ultimoMensajeAt;
    }

    public void setUltimoMensajeAt(OffsetDateTime ultimoMensajeAt) {
        this.ultimoMensajeAt = ultimoMensajeAt;
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
