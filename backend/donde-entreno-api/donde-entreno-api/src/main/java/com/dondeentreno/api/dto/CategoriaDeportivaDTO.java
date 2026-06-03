package com.dondeentreno.api.dto;

/**
 * DTO de CategoriaDeportiva.
 *
 * DTO significa Data Transfer Object.
 * Es el objeto que vamos a devolver desde la API hacia el frontend.
 *
 * No exponemos directamente la entidad de base de datos.
 * Devolvemos solamente los campos que nos interesan para mostrar.
 */
public class CategoriaDeportivaDTO {

    private Long id;
    private String nombre;
    private String slug;
    private String descripcion;
    private String iconoUrl;
    private Integer orden;

    public CategoriaDeportivaDTO() {
    }

    public CategoriaDeportivaDTO(
            Long id,
            String nombre,
            String slug,
            String descripcion,
            String iconoUrl,
            Integer orden
    ) {
        this.id = id;
        this.nombre = nombre;
        this.slug = slug;
        this.descripcion = descripcion;
        this.iconoUrl = iconoUrl;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIconoUrl() {
        return iconoUrl;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setIconoUrl(String iconoUrl) {
        this.iconoUrl = iconoUrl;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}