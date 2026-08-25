package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Comentario de un miembro sobre un aviso del grupo (script 38).
 *
 * Mismo molde que {@link ComentarioImagen}, incluidos sus cuatro
 * estados: asi la moderacion del grupo usa las mismas dos vias que ya
 * existen (ocultar por publicador y ocultar por admin).
 */
@Entity
@Table(name = "comentario_aviso")
public class ComentarioAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aviso_id", nullable = false)
    private Long avisoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "texto", nullable = false, length = 500)
    private String texto;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public ComentarioAviso() {
    }

    public Long getId() {
        return id;
    }

    public Long getAvisoId() {
        return avisoId;
    }

    public void setAvisoId(Long avisoId) {
        this.avisoId = avisoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
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
