package com.dondeentreno.api.dto;

/**
 * DTO de PerfilPublicador.
 *
 * Representa los datos públicos que vamos a devolver
 * desde la API hacia el frontend.
 *
 * No devolvemos datos internos del usuario dueño del perfil,
 * como email de login, passwordHash, rol, etc.
 */
public class PerfilPublicadorDTO {

    private Long id;
    private String nombre;
    private String tipoPublicador;
    private String descripcion;
    private String emailContacto;
    private String telefonoContacto;
    private String whatsapp;
    private String instagram;
    private String sitioWeb;
    private Boolean verificado;

    /**
     * Cantidad de usuarios que siguen al perfil.
     *
     * Campo aditivo: lo completa el service (con un query agrupado en
     * los listados, para no caer en N+1). Se deja fuera del constructor
     * con todos los campos para no romper a los llamadores existentes.
     */
    private Long cantidadSeguidores;

    /**
     * URL amigable del perfil (script 27). Null solo para perfiles
     * creados en la ventana de deploy de la migración. Aditivo.
     */
    private String slug;

    /**
     * URL del LOGO aprobado (o null): la identidad única del publicador
     * en listados y perfil (fix UX 2026-08-22). Aditivo, no viaja por
     * el constructor: lo asigna el service con un query batch.
     */
    private String logoUrl;

    public PerfilPublicadorDTO() {
    }

    public PerfilPublicadorDTO(
            Long id,
            String nombre,
            String tipoPublicador,
            String descripcion,
            String emailContacto,
            String telefonoContacto,
            String whatsapp,
            String instagram,
            String sitioWeb,
            Boolean verificado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.tipoPublicador = tipoPublicador;
        this.descripcion = descripcion;
        this.emailContacto = emailContacto;
        this.telefonoContacto = telefonoContacto;
        this.whatsapp = whatsapp;
        this.instagram = instagram;
        this.sitioWeb = sitioWeb;
        this.verificado = verificado;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public Boolean getVerificado() {
        return verificado;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setSitioWeb(String sitioWeb) {
        this.sitioWeb = sitioWeb;
    }

    public void setVerificado(Boolean verificado) {
        this.verificado = verificado;
    }

    public Long getCantidadSeguidores() {
        return cantidadSeguidores;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCantidadSeguidores(Long cantidadSeguidores) {
        this.cantidadSeguidores = cantidadSeguidores;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}