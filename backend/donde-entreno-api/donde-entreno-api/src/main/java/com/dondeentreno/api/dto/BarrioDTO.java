package com.dondeentreno.api.dto;

/**
 * DTO de Barrio.
 *
 * Este objeto representa los datos que vamos a devolver
 * desde la API hacia el frontend.
 *
 * No devolvemos directamente la entidad Barrio para evitar
 * exponer campos internos como createdAt o updatedAt.
 */
public class BarrioDTO {

    private Long id;
    private String nombre;

    private Long ciudadId;
    private String ciudadNombre;

    public BarrioDTO() {
    }

    public BarrioDTO(Long id, String nombre, Long ciudadId, String ciudadNombre) {
        this.id = id;
        this.nombre = nombre;
        this.ciudadId = ciudadId;
        this.ciudadNombre = ciudadNombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Long getCiudadId() {
        return ciudadId;
    }

    public String getCiudadNombre() {
        return ciudadNombre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCiudadId(Long ciudadId) {
        this.ciudadId = ciudadId;
    }

    public void setCiudadNombre(String ciudadNombre) {
        this.ciudadNombre = ciudadNombre;
    }
}