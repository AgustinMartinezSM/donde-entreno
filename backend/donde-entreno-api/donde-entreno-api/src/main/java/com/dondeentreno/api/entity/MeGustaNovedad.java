package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * "Me gusta" sobre una novedad del canal (script 37).
 *
 * Mismo molde que {@link MeGustaImagen}: la fila existe o no existe.
 * Sin tipo de reacción — una sola — y el UNIQUE de la migración es lo
 * que hace idempotente al botón.
 */
@Entity
@Table(name = "me_gusta_novedad")
public class MeGustaNovedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "novedad_id", nullable = false)
    private Long novedadId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public MeGustaNovedad() {
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

    public Long getNovedadId() {
        return novedadId;
    }

    public void setNovedadId(Long novedadId) {
        this.novedadId = novedadId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
