package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Like de un usuario sobre una foto (script 23, bloque 14).
 *
 * (usuario, imagen) es unico: dar like es idempotente y quitarlo borra
 * la fila — el patron exacto de favoritos. FKs planas por el mismo
 * motivo: este registro nunca navega a sus entidades.
 */
@Entity
@Table(name = "me_gusta_imagen")
public class MeGustaImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "imagen_id", nullable = false)
    private Long imagenId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public MeGustaImagen() {
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

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
