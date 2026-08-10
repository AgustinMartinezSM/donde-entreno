package com.dondeentreno.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Consulta que manda el asistente del frontend.
 *
 * El largo máximo del texto NO se valida acá con @Size porque es
 * configurable por variable de entorno
 * (DONDEENTRENO_ASISTENTE_MAX_INPUT_CHARS): lo controla AsistenteService.
 *
 * El historial sí tiene tope acá, y es defensivo: el servicio igual se
 * queda con los últimos N, pero un cliente cualquiera podría mandar mil
 * mensajes y no queremos ni deserializarlos.
 */
public class AsistenteConsultaRequestDTO {

    /** Tope duro de mensajes aceptados en el cuerpo. */
    private static final int MAX_HISTORIAL = 40;

    @NotBlank(message = "Escribí una consulta.")
    private String texto;

    /** Ruta donde estaba el usuario, para situar la respuesta. Opcional. */
    private String rutaActual;

    /**
     * Turnos previos de la conversación, en orden.
     *
     * Es la memoria del asistente y vive únicamente acá: no se guarda en
     * la base ni en memoria del servidor. Si el usuario cierra la pestaña,
     * la charla desaparece.
     */
    @Valid
    @Size(max = MAX_HISTORIAL, message = "El historial es demasiado largo.")
    private List<AsistenteMensajeDTO> historial;

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

    public List<AsistenteMensajeDTO> getHistorial() {
        return historial;
    }

    public void setHistorial(List<AsistenteMensajeDTO> historial) {
        this.historial = historial;
    }
}
