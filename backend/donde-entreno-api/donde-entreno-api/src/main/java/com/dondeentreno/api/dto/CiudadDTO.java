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

    public CiudadDTO() {
    }

    public CiudadDTO(Long id, String nombre, String provincia, String pais) {
        this.id = id;
        this.nombre = nombre;
        this.provincia = provincia;
        this.pais = pais;
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
}