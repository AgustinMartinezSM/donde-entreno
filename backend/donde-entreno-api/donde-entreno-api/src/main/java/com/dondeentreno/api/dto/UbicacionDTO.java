package com.dondeentreno.api.dto;

import java.math.BigDecimal;

/**
 * DTO de Ubicacion.
 *
 * Representa los datos públicos de una ubicación que vamos a devolver
 * desde la API hacia el frontend.
 *
 * No devolvemos directamente la entidad Ubicacion para evitar exponer
 * campos internos como createdAt, updatedAt o deletedAt.
 */
public class UbicacionDTO {

    private Long id;
    private String nombre;
    private String direccion;
    private String referencia;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String googleMapsUrl;

    private Long perfilPublicadorId;
    private String perfilPublicadorNombre;

    private Long ciudadId;
    private String ciudadNombre;

    private Long barrioId;
    private String barrioNombre;

    public UbicacionDTO() {
    }

    public UbicacionDTO(
            Long id,
            String nombre,
            String direccion,
            String referencia,
            BigDecimal latitud,
            BigDecimal longitud,
            String googleMapsUrl,
            Long perfilPublicadorId,
            String perfilPublicadorNombre,
            Long ciudadId,
            String ciudadNombre,
            Long barrioId,
            String barrioNombre
    ) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.referencia = referencia;
        this.latitud = latitud;
        this.longitud = longitud;
        this.googleMapsUrl = googleMapsUrl;
        this.perfilPublicadorId = perfilPublicadorId;
        this.perfilPublicadorNombre = perfilPublicadorNombre;
        this.ciudadId = ciudadId;
        this.ciudadNombre = ciudadNombre;
        this.barrioId = barrioId;
        this.barrioNombre = barrioNombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public String getPerfilPublicadorNombre() {
        return perfilPublicadorNombre;
    }

    public Long getCiudadId() {
        return ciudadId;
    }

    public String getCiudadNombre() {
        return ciudadNombre;
    }

    public Long getBarrioId() {
        return barrioId;
    }

    public String getBarrioNombre() {
        return barrioNombre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public void setPerfilPublicadorNombre(String perfilPublicadorNombre) {
        this.perfilPublicadorNombre = perfilPublicadorNombre;
    }

    public void setCiudadId(Long ciudadId) {
        this.ciudadId = ciudadId;
    }

    public void setCiudadNombre(String ciudadNombre) {
        this.ciudadNombre = ciudadNombre;
    }

    public void setBarrioId(Long barrioId) {
        this.barrioId = barrioId;
    }

    public void setBarrioNombre(String barrioNombre) {
        this.barrioNombre = barrioNombre;
    }
}
