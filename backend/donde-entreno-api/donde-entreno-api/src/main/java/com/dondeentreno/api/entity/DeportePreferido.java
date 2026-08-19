package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Un deporte elegido por un usuario en su perfil deportivo (script 20).
 *
 * (usuario, deporte) es unico. La UI edita el conjunto entero, asi que
 * el servicio reemplaza todas las filas del usuario de una vez.
 *
 * FKs planas por el mismo motivo que FavoritoActividad: los slugs del
 * listado salen de un query escalar con join, nunca de navegar la
 * entidad.
 */
@Entity
@Table(name = "deporte_preferido")
public class DeportePreferido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "deporte_id", nullable = false)
    private Long deporteId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public DeportePreferido() {
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

    public Long getDeporteId() {
        return deporteId;
    }

    public void setDeporteId(Long deporteId) {
        this.deporteId = deporteId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
