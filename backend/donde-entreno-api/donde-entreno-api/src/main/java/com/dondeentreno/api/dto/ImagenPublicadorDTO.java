package com.dondeentreno.api.dto;

import com.dondeentreno.api.entity.Imagen;

import java.time.OffsetDateTime;

/**
 * Imagen de una actividad vista desde el panel del publicador:
 * incluye el estado de moderación y el motivo de rechazo.
 */
public class ImagenPublicadorDTO {

    private Long id;
    private String url;
    private String tipoImagen;
    private String estadoModeracion;
    private String motivoRechazo;
    private Boolean activa;
    /* Para el orden manual y la edición de alt/epígrafe (fase 2). */
    private Integer orden;
    private String titulo;
    private String descripcion;
    private OffsetDateTime createdAt;

    public static ImagenPublicadorDTO desdeEntidad(Imagen imagen) {
        ImagenPublicadorDTO dto = new ImagenPublicadorDTO();
        dto.setId(imagen.getId());
        dto.setUrl(imagen.getUrl());
        dto.setTipoImagen(imagen.getTipoImagen());
        dto.setEstadoModeracion(imagen.getEstadoModeracion());
        dto.setMotivoRechazo(imagen.getMotivoRechazo());
        dto.setActiva(imagen.getActiva());
        dto.setOrden(imagen.getOrden());
        dto.setTitulo(imagen.getTitulo());
        dto.setDescripcion(imagen.getDescripcion());
        dto.setCreatedAt(imagen.getCreatedAt());
        return dto;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public String getEstadoModeracion() {
        return estadoModeracion;
    }

    public void setEstadoModeracion(String estadoModeracion) {
        this.estadoModeracion = estadoModeracion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
