package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Comentario visible de una foto (script 30). Autor como nombre de
 * pila + inicial (criterio de las valoraciones).
 */
public class ComentarioImagenDTO {

    private Long id;
    private String texto;
    private String autorNombre;
    private boolean esPropio;
    private OffsetDateTime createdAt;

    public ComentarioImagenDTO() {
    }

    public ComentarioImagenDTO(
            Long id,
            String texto,
            String autorNombre,
            boolean esPropio,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.texto = texto;
        this.autorNombre = autorNombre;
        this.esPropio = esPropio;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public boolean isEsPropio() {
        return esPropio;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
