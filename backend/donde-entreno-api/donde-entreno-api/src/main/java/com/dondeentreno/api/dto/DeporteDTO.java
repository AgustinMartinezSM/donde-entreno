package com.dondeentreno.api.dto;

/**
 * DTO de Deporte.
 *
 * Este objeto representa los datos que vamos a devolver
 * desde la API hacia el frontend.
 *
 * No devolvemos directamente la entidad Deporte para evitar
 * exponer detalles internos de la base de datos.
 */
public class DeporteDTO {

    private Long id;
    private String nombre;
    private String slug;
    private String descripcion;
    private String iconoUrl;
    private Integer orden;

    private Long categoriaId;
    private String categoriaNombre;
    private String categoriaSlug;

    public DeporteDTO() {
    }

    public DeporteDTO(
            Long id,
            String nombre,
            String slug,
            String descripcion,
            String iconoUrl,
            Integer orden,
            Long categoriaId,
            String categoriaNombre,
            String categoriaSlug
    ) {
        this.id = id;
        this.nombre = nombre;
        this.slug = slug;
        this.descripcion = descripcion;
        this.iconoUrl = iconoUrl;
        this.orden = orden;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.categoriaSlug = categoriaSlug;
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

    public Long getCategoriaId() {
        return categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public String getCategoriaSlug() {
        return categoriaSlug;
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

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public void setCategoriaSlug(String categoriaSlug) {
        this.categoriaSlug = categoriaSlug;
    }
}