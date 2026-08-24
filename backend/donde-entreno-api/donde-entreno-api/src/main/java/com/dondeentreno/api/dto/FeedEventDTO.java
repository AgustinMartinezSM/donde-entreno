package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Un hecho del feed, listo para pintar SIN llamadas extra (Fase 6):
 * trae la identidad del publicador y, si aplica, la actividad y la
 * foto. El frontend no tiene que resolver nada por su cuenta.
 */
public class FeedEventDTO {

    private Long id;
    private String tipo;
    private String resumen;
    private OffsetDateTime createdAt;

    /* Quién lo hizo. */
    private Long perfilPublicadorId;
    private String perfilNombre;
    private String perfilSlug;
    private String perfilLogoUrl;

    /* Sobre qué, cuando aplica. */
    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;
    private String actividadImagenUrl;

    /* La foto del hecho, cuando el tipo es de foto. */
    private Long imagenId;
    private String imagenUrl;

    public FeedEventDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
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

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
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

    public String getActividadImagenUrl() {
        return actividadImagenUrl;
    }

    public void setActividadImagenUrl(String actividadImagenUrl) {
        this.actividadImagenUrl = actividadImagenUrl;
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
