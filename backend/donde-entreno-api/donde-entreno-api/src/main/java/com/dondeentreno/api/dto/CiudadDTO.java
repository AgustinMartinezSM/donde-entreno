package com.dondeentreno.api.dto;

/**
 * DTO de Ciudad.
 *
 * Este objeto representa los datos de ciudad que vamos a devolver
 * desde la API hacia el frontend.
 *
 * No devolvemos directamente la entidad Ciudad para evitar exponer
 * campos internos como createdAt o updatedAt.
 */
public class CiudadDTO {

    private Long id;
    private String nombre;
    private String provincia;
    private String pais;
    private String slug;
    private Integer orden;
    private Boolean activa;

    public CiudadDTO() {
    }

    public CiudadDTO(
            Long id,
            String nombre,
            String provincia,
            String pais,
            String slug,
            Integer orden,
            Boolean activa
    ) {
        this.id = id;
        this.nombre = nombre;
        this.provincia = provincia;
        this.pais = pais;
        this.slug = slug;
        this.orden = orden;
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getPais() {
        return pais;
    }

    public String getSlug() {
        return slug;
    }

    public Integer getOrden() {
        return orden;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}
