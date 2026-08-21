package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request de visibilidad de una actividad del publicador (fase 6):
 * visible=false la pausa, visible=true la reanuda.
 */
public class CambiarVisibilidadRequestDTO {

    @NotNull(message = "El campo visible es obligatorio.")
    private Boolean visible;

    public CambiarVisibilidadRequestDTO() {
    }

    public CambiarVisibilidadRequestDTO(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
