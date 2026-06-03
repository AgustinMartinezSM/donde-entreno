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
}