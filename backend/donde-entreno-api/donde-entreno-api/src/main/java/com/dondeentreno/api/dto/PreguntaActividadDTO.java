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

    /**
     * Contexto de la actividad (Fase 5): solo se completa en el
     * listado del PERFIL, donde se mezclan preguntas de varias
     * actividades. En el detalle viajan en null.
     */
    private String actividadTitulo;
    private String actividadSlug;

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

    public String getActividadTitulo() {
        return actividadTitulo;
    }

    public void setActividadTitulo(String actividadTitulo) {
        this.actividadTitulo = actividadTitulo;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }
}
