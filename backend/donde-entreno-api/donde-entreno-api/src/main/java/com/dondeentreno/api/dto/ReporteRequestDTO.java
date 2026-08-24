package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del POST de reportes (script 28, Fase 2 social). Los
 * catálogos de tipo/motivo se validan en el service.
 */
public class ReporteRequestDTO {

    @NotBlank(message = "Falta el tipo de objeto reportado.")
    private String tipoObjeto;

    @NotNull(message = "Falta el objeto reportado.")
    private Long objetoId;

    @NotBlank(message = "Falta el motivo del reporte.")
    private String motivo;

    @Size(max = 280, message = "El detalle no puede superar los 280 caracteres.")
    private String detalle;

    public String getTipoObjeto() {
        return tipoObjeto;
    }

    public void setTipoObjeto(String tipoObjeto) {
        this.tipoObjeto = tipoObjeto;
    }

    public Long getObjetoId() {
        return objetoId;
    }

    public void setObjetoId(Long objetoId) {
        this.objetoId = objetoId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
