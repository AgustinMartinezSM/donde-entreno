package com.dondeentreno.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detalle de actividad real visible desde el panel del publicador autenticado.
 */
public class PublicadorActividadDetalleDTO {

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
    private String descripcion;
    private String enfoque;
    private Boolean requiereInscripcion;
    private Boolean cuposLimitados;
    private String nombreLugar;
    private String direccion;
    private String referenciaUbicacion;
    private String whatsapp;
    private String instagram;
    private String email;
    private Long perfilPublicadorId;
    private String perfilPublicadorNombre;
    private String perfilPublicadorTipo;
    private Long solicitudOrigenId;
    private String solicitudCodigoSeguimiento;
    private List<PublicadorActividadHorarioDTO> horarios;
    private List<PublicadorActividadImagenDTO> imagenes;

    public PublicadorActividadDetalleDTO() {
    }

    public PublicadorActividadDetalleDTO(
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
            String slugPublico,
            String descripcion,
            String enfoque,
            Boolean requiereInscripcion,
            Boolean cuposLimitados,
            String nombreLugar,
            String direccion,
            String referenciaUbicacion,
            String whatsapp,
            String instagram,
            String email,
            Long perfilPublicadorId,
            String perfilPublicadorNombre,
            String perfilPublicadorTipo,
            Long solicitudOrigenId,
            String solicitudCodigoSeguimiento,
            List<PublicadorActividadHorarioDTO> horarios,
            List<PublicadorActividadImagenDTO> imagenes
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
        this.descripcion = descripcion;
        this.enfoque = enfoque;
        this.requiereInscripcion = requiereInscripcion;
        this.cuposLimitados = cuposLimitados;
        this.nombreLugar = nombreLugar;
        this.direccion = direccion;
        this.referenciaUbicacion = referenciaUbicacion;
        this.whatsapp = whatsapp;
        this.instagram = instagram;
        this.email = email;
        this.perfilPublicadorId = perfilPublicadorId;
        this.perfilPublicadorNombre = perfilPublicadorNombre;
        this.perfilPublicadorTipo = perfilPublicadorTipo;
        this.solicitudOrigenId = solicitudOrigenId;
        this.solicitudCodigoSeguimiento = solicitudCodigoSeguimiento;
        this.horarios = horarios;
        this.imagenes = imagenes;
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

    public String getDescripcion() {
        return descripcion;
    }

    public String getEnfoque() {
        return enfoque;
    }

    public Boolean getRequiereInscripcion() {
        return requiereInscripcion;
    }

    public Boolean getCuposLimitados() {
        return cuposLimitados;
    }

    public String getNombreLugar() {
        return nombreLugar;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferenciaUbicacion() {
        return referenciaUbicacion;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getEmail() {
        return email;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public String getPerfilPublicadorNombre() {
        return perfilPublicadorNombre;
    }

    public String getPerfilPublicadorTipo() {
        return perfilPublicadorTipo;
    }

    public Long getSolicitudOrigenId() {
        return solicitudOrigenId;
    }

    public String getSolicitudCodigoSeguimiento() {
        return solicitudCodigoSeguimiento;
    }

    public List<PublicadorActividadHorarioDTO> getHorarios() {
        return horarios;
    }

    public List<PublicadorActividadImagenDTO> getImagenes() {
        return imagenes;
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

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setEnfoque(String enfoque) {
        this.enfoque = enfoque;
    }

    public void setRequiereInscripcion(Boolean requiereInscripcion) {
        this.requiereInscripcion = requiereInscripcion;
    }

    public void setCuposLimitados(Boolean cuposLimitados) {
        this.cuposLimitados = cuposLimitados;
    }

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setReferenciaUbicacion(String referenciaUbicacion) {
        this.referenciaUbicacion = referenciaUbicacion;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public void setPerfilPublicadorNombre(String perfilPublicadorNombre) {
        this.perfilPublicadorNombre = perfilPublicadorNombre;
    }

    public void setPerfilPublicadorTipo(String perfilPublicadorTipo) {
        this.perfilPublicadorTipo = perfilPublicadorTipo;
    }

    public void setSolicitudOrigenId(Long solicitudOrigenId) {
        this.solicitudOrigenId = solicitudOrigenId;
    }

    public void setSolicitudCodigoSeguimiento(String solicitudCodigoSeguimiento) {
        this.solicitudCodigoSeguimiento = solicitudCodigoSeguimiento;
    }

    public void setHorarios(List<PublicadorActividadHorarioDTO> horarios) {
        this.horarios = horarios;
    }

    public void setImagenes(List<PublicadorActividadImagenDTO> imagenes) {
        this.imagenes = imagenes;
    }
}
