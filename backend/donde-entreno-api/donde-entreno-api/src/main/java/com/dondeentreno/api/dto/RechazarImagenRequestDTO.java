package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rechazo administrativo de una imagen: el motivo es obligatorio
 * y se le muestra al publicador.
 */
public class RechazarImagenRequestDTO {

    @NotBlank(message = "El motivo del rechazo es obligatorio.")
    @Size(max = 2000, message = "El motivo no puede superar los 2000 caracteres.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
