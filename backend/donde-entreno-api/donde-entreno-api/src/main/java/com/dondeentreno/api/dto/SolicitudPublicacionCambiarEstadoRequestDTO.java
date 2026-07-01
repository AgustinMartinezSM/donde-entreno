package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de entrada para cambiar el estado admin de una solicitud de publicacion.
 */
public class SolicitudPublicacionCambiarEstadoRequestDTO {

    @NotBlank(message = "El estado es obligatorio.")
    @Pattern(
            regexp = "(?i)\\s*(EN_REVISION|RECHAZADA)\\s*",
            message = "El estado debe ser EN_REVISION o RECHAZADA."
    )
    private String estado;

    private String motivoRechazo;

    public SolicitudPublicacionCambiarEstadoRequestDTO() {
    }

    public SolicitudPublicacionCambiarEstadoRequestDTO(String estado, String motivoRechazo) {
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}
