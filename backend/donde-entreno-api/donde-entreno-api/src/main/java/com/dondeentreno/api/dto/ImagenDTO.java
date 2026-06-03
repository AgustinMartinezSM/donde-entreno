package com.dondeentreno.api.dto;

/**
 * DTO de Imagen.
 *
 * Representa los datos públicos de una imagen que vamos a devolver
 * desde la API hacia el frontend.
 *
 * No devolvemos directamente la entidad Imagen.
 */
public class ImagenDTO {

    private Long id;
    private String url;
    private String tipoImagen;
    private String titulo;
    private String descripcion;
    private Integer orden;

    private Long actividadId;
    private String actividadSlug;

    private Long perfilPublicadorId;
    private String perfilPublicadorNombre;

    public ImagenDTO() {
    }

    public ImagenDTO(
            Long id,
            String url,
            String tipoImagen,
            String titulo,
            String descripcion,
            Integer orden,
            Long actividadId,
            String actividadSlug,
            Long perfilPublicadorId,
            String perfilPublicadorNombre
    ) {
        this.id = id;
        this.url = url;
        this.tipoImagen = tipoImagen;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.orden = orden;
        this.actividadId = actividadId;
        this.actividadSlug = actividadSlug;
        this.perfilPublicadorId = perfilPublicadorId;
        this.perfilPublicadorNombre = perfilPublicadorNombre;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getOrden() {
        return orden;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public String getPerfilPublicadorNombre() {
        return perfilPublicadorNombre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public void setPerfilPublicadorNombre(String perfilPublicadorNombre) {
        this.perfilPublicadorNombre = perfilPublicadorNombre;
    }
}
