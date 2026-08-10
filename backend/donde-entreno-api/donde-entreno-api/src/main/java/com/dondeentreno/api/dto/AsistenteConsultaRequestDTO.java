package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Consulta que manda el asistente del frontend.
 *
 * El largo máximo NO se valida acá con @Size porque es configurable por
 * variable de entorno (DONDEENTRENO_ASISTENTE_MAX_INPUT_CHARS): lo
 * controla AsistenteService.
 */
public class AsistenteConsultaRequestDTO {

    @NotBlank(message = "Escribí una consulta.")
    private String texto;

    /** Ruta donde estaba el usuario, para situar la respuesta. Opcional. */
    private String rutaActual;

    public AsistenteConsultaRequestDTO() {
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getRutaActual() {
        return rutaActual;
    }

    public void setRutaActual(String rutaActual) {
        this.rutaActual = rutaActual;
    }
}
