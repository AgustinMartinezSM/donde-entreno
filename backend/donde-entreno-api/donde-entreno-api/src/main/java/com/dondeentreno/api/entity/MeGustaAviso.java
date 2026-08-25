package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Reaccion a un aviso del grupo (script 38). Mismo molde que
 * {@link MeGustaNovedad}: la fila existe o no existe.
 */
@Entity
@Table(name = "me_gusta_aviso")
public class MeGustaAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "aviso_id", nullable = false)
    private Long avisoId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public MeGustaAviso() {
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

    public Long getAvisoId() {
        return avisoId;
    }

    public void setAvisoId(Long avisoId) {
        this.avisoId = avisoId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
