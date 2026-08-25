package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Un comentario del grupo, listo para pintar. El autor viaja con
 * nombre corto ("Ana G."), igual que en los comentarios de fotos.
 */
public class ComentarioAvisoDTO {

    private Long id;
    private String texto;
    private String autorNombre;
    private Boolean esPropio;
    private OffsetDateTime createdAt;

    public ComentarioAvisoDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public Boolean getEsPropio() {
        return esPropio;
    }

    public void setEsPropio(Boolean esPropio) {
        this.esPropio = esPropio;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
