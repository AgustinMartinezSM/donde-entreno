package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Novedad del canal, lista para pintar (Fase 8): trae la identidad del
 * publicador y la foto resuelta, sin llamadas extra.
 */
public class NovedadDTO {

    private Long id;
    private String texto;
    private OffsetDateTime createdAt;

    private Long perfilPublicadorId;
    private String perfilNombre;
    private String perfilSlug;
    private String perfilLogoUrl;

    private Long imagenId;
    private String imagenUrl;

    public NovedadDTO() {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public String getPerfilNombre() {
        return perfilNombre;
    }

    public void setPerfilNombre(String perfilNombre) {
        this.perfilNombre = perfilNombre;
    }

    public String getPerfilSlug() {
        return perfilSlug;
    }

    public void setPerfilSlug(String perfilSlug) {
        this.perfilSlug = perfilSlug;
    }

    public String getPerfilLogoUrl() {
        return perfilLogoUrl;
    }

    public void setPerfilLogoUrl(String perfilLogoUrl) {
        this.perfilLogoUrl = perfilLogoUrl;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
