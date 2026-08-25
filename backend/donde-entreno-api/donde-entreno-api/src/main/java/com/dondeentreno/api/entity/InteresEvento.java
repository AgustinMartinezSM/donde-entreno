package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * "Me interesa" sobre un evento (script 35, Fase 9).
 *
 * Tabla propia y NO `interes_actividad`: los estados de esa tabla
 * (QUIERO_PROBAR / YA_PROBE) no significan nada sobre algo que pasa
 * una sola vez. Acá la fila existe o no existe, y el UNIQUE de la
 * migración es lo que hace idempotente al botón.
 */
@Entity
@Table(name = "interes_evento")
public class InteresEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "evento_deportivo_id", nullable = false)
    private Long eventoDeportivoId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public InteresEvento() {
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

    public Long getEventoDeportivoId() {
        return eventoDeportivoId;
    }

    public void setEventoDeportivoId(Long eventoDeportivoId) {
        this.eventoDeportivoId = eventoDeportivoId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
