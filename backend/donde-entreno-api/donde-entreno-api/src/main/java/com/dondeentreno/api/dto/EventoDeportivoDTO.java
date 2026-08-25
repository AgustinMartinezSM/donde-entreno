package com.dondeentreno.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Un evento listo para pintar (Fase 9): trae la identidad del
 * publicador, el deporte, la sede con su punto y el contador de
 * interesados, sin que el frontend tenga que pedir nada más.
 */
public class EventoDeportivoDTO {

    private Long id;
    private String slug;
    private String titulo;
    private String descripcion;

    private OffsetDateTime iniciaAt;
    private OffsetDateTime terminaAt;

    private Integer cupo;
    private Boolean esGratis;
    private BigDecimal precioReferencia;
    private Boolean mostrarPrecio;

    /** PUBLICADO o CANCELADO (lo oculto y lo borrado no sale). */
    private String estado;

    private Long perfilPublicadorId;
    private String perfilNombre;
    private String perfilSlug;
    private String perfilLogoUrl;
    private String whatsappContacto;

    private Long deporteId;
    private String deporteNombre;
    private String deporteSlug;

    private String sedeNombre;
    private String direccion;
    private String ciudadNombre;
    private String ciudadSlug;
    private String barrioNombre;
    private BigDecimal latitud;
    private BigDecimal longitud;

    /* Solo si el evento cuelga de una actividad. */
    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;

    private Long imagenId;
    private String imagenUrl;

    private Long cantidadInteresados;
    /** Con sesión: si el usuario ya marcó "me interesa". */
    private Boolean meInteresa;

    public EventoDeportivoDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public OffsetDateTime getIniciaAt() {
        return iniciaAt;
    }

    public void setIniciaAt(OffsetDateTime iniciaAt) {
        this.iniciaAt = iniciaAt;
    }

    public OffsetDateTime getTerminaAt() {
        return terminaAt;
    }

    public void setTerminaAt(OffsetDateTime terminaAt) {
        this.terminaAt = terminaAt;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public Boolean getEsGratis() {
        return esGratis;
    }

    public void setEsGratis(Boolean esGratis) {
        this.esGratis = esGratis;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public String getPerfilNombre() {
        return perfilNombre;
    }

    public void setPerfilNombre(String perfilNombre) {
        this.perfilNombre = perfilNombre;
    }

    public String getPerfilSlug() {
        return perfilSlug;
    }

    public void setPerfilSlug(String perfilSlug) {
        this.perfilSlug = perfilSlug;
    }

    public String getPerfilLogoUrl() {
        return perfilLogoUrl;
    }

    public void setPerfilLogoUrl(String perfilLogoUrl) {
        this.perfilLogoUrl = perfilLogoUrl;
    }

    public String getWhatsappContacto() {
        return whatsappContacto;
    }

    public void setWhatsappContacto(String whatsappContacto) {
        this.whatsappContacto = whatsappContacto;
    }

    public Long getDeporteId() {
        return deporteId;
    }

    public void setDeporteId(Long deporteId) {
        this.deporteId = deporteId;
    }

    public String getDeporteNombre() {
        return deporteNombre;
    }

    public void setDeporteNombre(String deporteNombre) {
        this.deporteNombre = deporteNombre;
    }

    public String getDeporteSlug() {
        return deporteSlug;
    }

    public void setDeporteSlug(String deporteSlug) {
        this.deporteSlug = deporteSlug;
    }

    public String getSedeNombre() {
        return sedeNombre;
    }

    public void setSedeNombre(String sedeNombre) {
        this.sedeNombre = sedeNombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudadNombre() {
        return ciudadNombre;
    }

    public void setCiudadNombre(String ciudadNombre) {
        this.ciudadNombre = ciudadNombre;
    }

    public String getCiudadSlug() {
        return ciudadSlug;
    }

    public void setCiudadSlug(String ciudadSlug) {
        this.ciudadSlug = ciudadSlug;
    }

    public String getBarrioNombre() {
        return barrioNombre;
    }

    public void setBarrioNombre(String barrioNombre) {
        this.barrioNombre = barrioNombre;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public String getActividadTitulo() {
        return actividadTitulo;
    }

    public void setActividadTitulo(String actividadTitulo) {
        this.actividadTitulo = actividadTitulo;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Long getCantidadInteresados() {
        return cantidadInteresados;
    }

    public void setCantidadInteresados(Long cantidadInteresados) {
        this.cantidadInteresados = cantidadInteresados;
    }

    public Boolean getMeInteresa() {
        return meInteresa;
    }

    public void setMeInteresa(Boolean meInteresa) {
        this.meInteresa = meInteresa;
    }
}
