package com.dondeentreno.api.dto;

/**
 * Imagen de actividad visible desde el panel del publicador autenticado.
 */
public class PublicadorActividadImagenDTO {

    private Long id;
    private String url;
    private String tipoImagen;
    private String titulo;
    private String descripcion;
    private Integer orden;

    public PublicadorActividadImagenDTO() {
    }

    public PublicadorActividadImagenDTO(
            Long id,
            String url,
            String tipoImagen,
            String titulo,
            String descripcion,
            Integer orden
    ) {
        this.id = id;
        this.url = url;
        this.tipoImagen = tipoImagen;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.orden = orden;
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
}
