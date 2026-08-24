package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Pregunta pública con su respuesta (script 29). Sin nombre del autor
 * a propósito: la pregunta ayuda a todos, quién la hizo no importa.
 */
public class PreguntaActividadDTO {

    private Long id;
    private String pregunta;
    private String respuesta;
    private OffsetDateTime respondidaAt;
    private boolean esPropia;
    private OffsetDateTime createdAt;

    public PreguntaActividadDTO() {
    }

    public PreguntaActividadDTO(
            Long id,
            String pregunta,
            String respuesta,
            OffsetDateTime respondidaAt,
            boolean esPropia,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.respondidaAt = respondidaAt;
        this.esPropia = esPropia;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPregunta() {
        return pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public OffsetDateTime getRespondidaAt() {
        return respondidaAt;
    }

    public boolean isEsPropia() {
        return esPropia;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
