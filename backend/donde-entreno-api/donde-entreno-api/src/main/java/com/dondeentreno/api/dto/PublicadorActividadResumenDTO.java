package com.dondeentreno.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resumen de actividad real visible desde el panel del publicador autenticado.
 */
public class PublicadorActividadResumenDTO {

    private Long id;
    private String titulo;
    private String slug;
    private String deporteNombre;
    private String deporteSlug;
    private String categoriaDeportivaNombre;
    private String ciudadNombre;
    private String ciudadSlug;
    private String barrioNombre;
    private String estadoPublicacion;
    private Boolean activa;
    private String modalidad;
    private String nivel;
    private Integer edadMinima;
    private Integer edadMaxima;
    private BigDecimal precioReferencia;
    private Boolean mostrarPrecio;
    private String imagenPrincipalUrl;
    private OffsetDateTime createdAt;
    private String slugPublico;

    public PublicadorActividadResumenDTO() {
    }

    public PublicadorActividadResumenDTO(
            Long id,
            String titulo,
            String slug,
            String deporteNombre,
            String deporteSlug,
            String categoriaDeportivaNombre,
            String ciudadNombre,
            String ciudadSlug,
            String barrioNombre,
            String estadoPublicacion,
            Boolean activa,
            String modalidad,
            String nivel,
            Integer edadMinima,
            Integer edadMaxima,
            BigDecimal precioReferencia,
            Boolean mostrarPrecio,
            String imagenPrincipalUrl,
            OffsetDateTime createdAt,
            String slugPublico
    ) {
        this.id = id;
        this.titulo = titulo;
        this.slug = slug;
        this.deporteNombre = deporteNombre;
        this.deporteSlug = deporteSlug;
        this.categoriaDeportivaNombre = categoriaDeportivaNombre;
        this.ciudadNombre = ciudadNombre;
        this.ciudadSlug = ciudadSlug;
        this.barrioNombre = barrioNombre;
        this.estadoPublicacion = estadoPublicacion;
        this.activa = activa;
        this.modalidad = modalidad;
        this.nivel = nivel;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
        this.precioReferencia = precioReferencia;
        this.mostrarPrecio = mostrarPrecio;
        this.imagenPrincipalUrl = imagenPrincipalUrl;
        this.createdAt = createdAt;
        this.slugPublico = slugPublico;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSlug() {
        return slug;
    }

    public String getDeporteNombre() {
        return deporteNombre;
    }

    public String getDeporteSlug() {
        return deporteSlug;
    }

    public String getCategoriaDeportivaNombre() {
        return categoriaDeportivaNombre;
    }

    public String getCiudadNombre() {
        return ciudadNombre;
    }

    public String getCiudadSlug() {
        return ciudadSlug;
    }

    public String getBarrioNombre() {
        return barrioNombre;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public Boolean getActiva() {
        return activa;
    }

    public String getModalidad() {
        return modalidad;
    }

    public String getNivel() {
        return nivel;
    }

    public Integer getEdadMinima() {
        return edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public String getImagenPrincipalUrl() {
        return imagenPrincipalUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getSlugPublico() {
        return slugPublico;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setDeporteNombre(String deporteNombre) {
        this.deporteNombre = deporteNombre;
    }

    public void setDeporteSlug(String deporteSlug) {
        this.deporteSlug = deporteSlug;
    }

    public void setCategoriaDeportivaNombre(String categoriaDeportivaNombre) {
        this.categoriaDeportivaNombre = categoriaDeportivaNombre;
    }

    public void setCiudadNombre(String ciudadNombre) {
        this.ciudadNombre = ciudadNombre;
    }

    public void setCiudadSlug(String ciudadSlug) {
        this.ciudadSlug = ciudadSlug;
    }

    public void setBarrioNombre(String barrioNombre) {
        this.barrioNombre = barrioNombre;
    }

    public void setEstadoPublicacion(String estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public void setEdadMinima(Integer edadMinima) {
        this.edadMinima = edadMinima;
    }

    public void setEdadMaxima(Integer edadMaxima) {
        this.edadMaxima = edadMaxima;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
    }

    public void setImagenPrincipalUrl(String imagenPrincipalUrl) {
        this.imagenPrincipalUrl = imagenPrincipalUrl;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setSlugPublico(String slugPublico) {
        this.slugPublico = slugPublico;
    }
}
