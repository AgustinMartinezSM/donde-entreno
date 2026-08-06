package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de estado administrativo de una solicitud de cambio:
 * EN_REVISION o RECHAZADA (con motivo obligatorio).
 * La aprobacion tiene su propio endpoint.
 */
public class ActualizarEstadoSolicitudCambioRequestDTO {

    @NotBlank(message = "El estado es obligatorio.")
    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado;

    @Size(max = 2000, message = "El motivo de rechazo no puede superar los 2000 caracteres.")
    private String motivoRechazo;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}
