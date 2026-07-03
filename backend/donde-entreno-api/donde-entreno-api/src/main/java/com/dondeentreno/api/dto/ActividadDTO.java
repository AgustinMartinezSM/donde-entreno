package com.dondeentreno.api.dto;

import java.math.BigDecimal;

/**
 * DTO de Actividad.
 *
 * Representa los datos públicos de una actividad deportiva
 * que vamos a devolver desde la API hacia el frontend.
 *
 * No devolvemos directamente la entidad Actividad para evitar
 * exponer campos internos como deletedAt, createdAt, updatedAt,
 * motivoRechazo o estadoPublicacion.
 */
public class ActividadDTO {

    private Long id;
    private String titulo;
    private String slug;
    private String descripcion;

    private Integer edadMinima;
    private Integer edadMaxima;

    private String nivel;
    private String enfoque;
    private String modalidad;

    private BigDecimal precioReferencia;
    private Boolean mostrarPrecio;
    private Boolean requiereInscripcion;
    private Boolean cuposLimitados;

    private String whatsappContacto;
    private String instagramContacto;
    private String emailContacto;

    private Long perfilPublicadorId;
    private String perfilPublicadorNombre;
    private String tipoPublicador;
    private Boolean perfilVerificado;

    private Long deporteId;
    private String deporteNombre;
    private String deporteSlug;

    private Long categoriaDeportivaId;
    private String categoriaDeportivaNombre;
    private String categoriaDeportivaSlug;

    private Long ubicacionId;
    private String ubicacionNombre;
    private String direccion;

    private Long ciudadId;
    private String ciudadNombre;
    private String ciudadSlug;

    private Long barrioId;
    private String barrioNombre;

    public ActividadDTO() {
    }

    public ActividadDTO(
            Long id,
            String titulo,
            String slug,
            String descripcion,
            Integer edadMinima,
            Integer edadMaxima,
            String nivel,
            String enfoque,
            String modalidad,
            BigDecimal precioReferencia,
            Boolean mostrarPrecio,
            Boolean requiereInscripcion,
            Boolean cuposLimitados,
            String whatsappContacto,
            String instagramContacto,
            String emailContacto,
            Long perfilPublicadorId,
            String perfilPublicadorNombre,
            String tipoPublicador,
            Boolean perfilVerificado,
            Long deporteId,
            String deporteNombre,
            String deporteSlug,
            Long categoriaDeportivaId,
            String categoriaDeportivaNombre,
            String categoriaDeportivaSlug,
            Long ubicacionId,
            String ubicacionNombre,
            String direccion,
            Long ciudadId,
            String ciudadNombre,
            String ciudadSlug,
            Long barrioId,
            String barrioNombre
    ) {
        this.id = id;
        this.titulo = titulo;
        this.slug = slug;
        this.descripcion = descripcion;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
        this.nivel = nivel;
        this.enfoque = enfoque;
        this.modalidad = modalidad;
        this.precioReferencia = precioReferencia;
        this.mostrarPrecio = mostrarPrecio;
        this.requiereInscripcion = requiereInscripcion;
        this.cuposLimitados = cuposLimitados;
        this.whatsappContacto = whatsappContacto;
        this.instagramContacto = instagramContacto;
        this.emailContacto = emailContacto;
        this.perfilPublicadorId = perfilPublicadorId;
        this.perfilPublicadorNombre = perfilPublicadorNombre;
        this.tipoPublicador = tipoPublicador;
        this.perfilVerificado = perfilVerificado;
        this.deporteId = deporteId;
        this.deporteNombre = deporteNombre;
        this.deporteSlug = deporteSlug;
        this.categoriaDeportivaId = categoriaDeportivaId;
        this.categoriaDeportivaNombre = categoriaDeportivaNombre;
        this.categoriaDeportivaSlug = categoriaDeportivaSlug;
        this.ubicacionId = ubicacionId;
        this.ubicacionNombre = ubicacionNombre;
        this.direccion = direccion;
        this.ciudadId = ciudadId;
        this.ciudadNombre = ciudadNombre;
        this.ciudadSlug = ciudadSlug;
        this.barrioId = barrioId;
        this.barrioNombre = barrioNombre;
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

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getEdadMinima() {
        return edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public String getNivel() {
        return nivel;
    }

    public String getEnfoque() {
        return enfoque;
    }

    public String getModalidad() {
        return modalidad;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public Boolean getRequiereInscripcion() {
        return requiereInscripcion;
    }

    public Boolean getCuposLimitados() {
        return cuposLimitados;
    }

    public String getWhatsappContacto() {
        return whatsappContacto;
    }

    public String getInstagramContacto() {
        return instagramContacto;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public String getPerfilPublicadorNombre() {
        return perfilPublicadorNombre;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public Boolean getPerfilVerificado() {
        return perfilVerificado;
    }

    public Long getDeporteId() {
        return deporteId;
    }

    public String getDeporteNombre() {
        return deporteNombre;
    }

    public String getDeporteSlug() {
        return deporteSlug;
    }

    public Long getCategoriaDeportivaId() {
        return categoriaDeportivaId;
    }

    public String getCategoriaDeportivaNombre() {
        return categoriaDeportivaNombre;
    }

    public String getCategoriaDeportivaSlug() {
        return categoriaDeportivaSlug;
    }

    public Long getUbicacionId() {
        return ubicacionId;
    }

    public String getUbicacionNombre() {
        return ubicacionNombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public Long getCiudadId() {
        return ciudadId;
    }

    public String getCiudadNombre() {
        return ciudadNombre;
    }

    public String getCiudadSlug() {
        return ciudadSlug;
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

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setEdadMinima(Integer edadMinima) {
        this.edadMinima = edadMinima;
    }

    public void setEdadMaxima(Integer edadMaxima) {
        this.edadMaxima = edadMaxima;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public void setEnfoque(String enfoque) {
        this.enfoque = enfoque;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
    }

    public void setRequiereInscripcion(Boolean requiereInscripcion) {
        this.requiereInscripcion = requiereInscripcion;
    }

    public void setCuposLimitados(Boolean cuposLimitados) {
        this.cuposLimitados = cuposLimitados;
    }

    public void setWhatsappContacto(String whatsappContacto) {
        this.whatsappContacto = whatsappContacto;
    }

    public void setInstagramContacto(String instagramContacto) {
        this.instagramContacto = instagramContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public void setPerfilPublicadorNombre(String perfilPublicadorNombre) {
        this.perfilPublicadorNombre = perfilPublicadorNombre;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public void setPerfilVerificado(Boolean perfilVerificado) {
        this.perfilVerificado = perfilVerificado;
    }

    public void setDeporteId(Long deporteId) {
        this.deporteId = deporteId;
    }

    public void setDeporteNombre(String deporteNombre) {
        this.deporteNombre = deporteNombre;
    }

    public void setDeporteSlug(String deporteSlug) {
        this.deporteSlug = deporteSlug;
    }

    public void setCategoriaDeportivaId(Long categoriaDeportivaId) {
        this.categoriaDeportivaId = categoriaDeportivaId;
    }

    public void setCategoriaDeportivaNombre(String categoriaDeportivaNombre) {
        this.categoriaDeportivaNombre = categoriaDeportivaNombre;
    }

    public void setCategoriaDeportivaSlug(String categoriaDeportivaSlug) {
        this.categoriaDeportivaSlug = categoriaDeportivaSlug;
    }

    public void setUbicacionId(Long ubicacionId) {
        this.ubicacionId = ubicacionId;
    }

    public void setUbicacionNombre(String ubicacionNombre) {
        this.ubicacionNombre = ubicacionNombre;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setCiudadId(Long ciudadId) {
        this.ciudadId = ciudadId;
    }

    public void setCiudadNombre(String ciudadNombre) {
        this.ciudadNombre = ciudadNombre;
    }

    public void setCiudadSlug(String ciudadSlug) {
        this.ciudadSlug = ciudadSlug;
    }

    public void setBarrioId(Long barrioId) {
        this.barrioId = barrioId;
    }

    public void setBarrioNombre(String barrioNombre) {
        this.barrioNombre = barrioNombre;
    }
}
