package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Reporte para la cola del admin (script 28, Fase 2 social). No
 * expone al usuario que reporta: el admin modera contenido, no
 * personas.
 */
public class ReporteAdminDTO {

    private Long id;
    private String tipoObjeto;
    private Long objetoId;
    private String motivo;
    private String detalle;
    private String estado;
    private OffsetDateTime createdAt;

    public ReporteAdminDTO() {
    }

    public ReporteAdminDTO(
            Long id,
            String tipoObjeto,
            Long objetoId,
            String motivo,
            String detalle,
            String estado,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.tipoObjeto = tipoObjeto;
        this.objetoId = objetoId;
        this.motivo = motivo;
        this.detalle = detalle;
        this.estado = estado;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTipoObjeto() {
        return tipoObjeto;
    }

    public Long getObjetoId() {
        return objetoId;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
