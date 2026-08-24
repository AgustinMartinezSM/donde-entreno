package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Valoración visible en el detalle público (script 29). El autor
 * aparece como nombre de pila + inicial del apellido: publicar una
 * reseña es un acto público explícito, pero no exponemos el apellido
 * completo.
 */
public class ValoracionPublicaDTO {

    private Long id;
    private int puntaje;
    private String comentario;
    private List<String> tags;
    private boolean verificada;
    private String autorNombre;
    private boolean esPropia;
    private OffsetDateTime createdAt;

    public ValoracionPublicaDTO() {
    }

    public ValoracionPublicaDTO(
            Long id,
            int puntaje,
            String comentario,
            List<String> tags,
            boolean verificada,
            String autorNombre,
            boolean esPropia,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.tags = tags;
        this.verificada = verificada;
        this.autorNombre = autorNombre;
        this.esPropia = esPropia;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isVerificada() {
        return verificada;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public boolean isEsPropia() {
        return esPropia;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
