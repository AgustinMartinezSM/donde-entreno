package com.dondeentreno.api.dto;

/**
 * Estado de seguimiento de un usuario respecto de un publicador.
 * Lo usa el botón "Seguir" para saber cómo mostrarse.
 */
public class EstadoSeguimientoDTO {

    private final boolean siguiendo;

    public EstadoSeguimientoDTO(boolean siguiendo) {
        this.siguiendo = siguiendo;
    }

    public boolean isSiguiendo() {
        return siguiendo;
    }
}
