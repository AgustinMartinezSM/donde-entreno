package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Notificación interna para la campanita (script 28, Fase 2 social).
 */
public class NotificacionDTO {

    private Long id;
    private String tipo;
    private String titulo;
    private String ruta;
    private boolean leida;
    private OffsetDateTime createdAt;

    public NotificacionDTO() {
    }

    public NotificacionDTO(
            Long id,
            String tipo,
            String titulo,
            String ruta,
            boolean leida,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.ruta = ruta;
        this.leida = leida;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getRuta() {
        return ruta;
    }

    public boolean isLeida() {
        return leida;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
