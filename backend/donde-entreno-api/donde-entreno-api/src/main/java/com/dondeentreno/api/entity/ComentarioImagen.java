package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Comentario en una foto (script 30): el primer texto libre de la
 * comunidad. Publica directo; se modera por estados. FKs planas.
 */
@Entity
@Table(name = "comentario_imagen")
public class ComentarioImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imagen_id", nullable = false)
    private Long imagenId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "texto", nullable = false, length = 500)
    private String texto;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "VISIBLE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public ComentarioImagen() {
    }

    public Long getId() {
        return id;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
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
