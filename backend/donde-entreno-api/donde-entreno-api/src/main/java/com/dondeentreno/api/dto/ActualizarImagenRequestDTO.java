package com.dondeentreno.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Edición del título y la descripción de una imagen propia. Los dos
 * campos alimentan el texto alternativo/epígrafe de las vistas
 * públicas. Semántica PATCH: null = no tocar; vacío/espacios = limpiar.
 */
public class ActualizarImagenRequestDTO {

    @Size(max = 150, message = "El titulo no puede superar los 150 caracteres.")
    private String titulo;

    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres.")
    private String descripcion;

    /* Fase 4 (script 30): catálogo validado en el service; "" = General. */
    @Size(max = 30, message = "La seccion no puede superar los 30 caracteres.")
    private String seccion;

    /** Toggle de comentarios de la foto. Null = no tocar. */
    private Boolean comentariosActivados;

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

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public Boolean getComentariosActivados() {
        return comentariosActivados;
    }

    public void setComentariosActivados(Boolean comentariosActivados) {
        this.comentariosActivados = comentariosActivados;
    }
}
