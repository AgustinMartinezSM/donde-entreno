package com.dondeentreno.api.dto;

import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;

/**
 * Datos del perfil publicador del usuario autenticado.
 */
public class PerfilPublicadorActualDTO {

    private Long id;
    private String nombre;
    private String tipoPublicador;
    private String estado;
    private Long ciudadPrincipalId;
    private String ciudadPrincipalNombre;
    private String whatsapp;
    private String instagram;
    private String emailContacto;
    private String telefonoContacto;
    private String descripcion;
    private Boolean activo;
    private Boolean verificado;

    public PerfilPublicadorActualDTO() {
    }

    public PerfilPublicadorActualDTO(
            Long id,
            String nombre,
            String tipoPublicador,
            String estado,
            Long ciudadPrincipalId,
            String ciudadPrincipalNombre,
            String whatsapp,
            String instagram,
            String emailContacto,
            String telefonoContacto,
            String descripcion,
            Boolean activo,
            Boolean verificado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.tipoPublicador = tipoPublicador;
        this.estado = estado;
        this.ciudadPrincipalId = ciudadPrincipalId;
        this.ciudadPrincipalNombre = ciudadPrincipalNombre;
        this.whatsapp = whatsapp;
        this.instagram = instagram;
        this.emailContacto = emailContacto;
        this.telefonoContacto = telefonoContacto;
        this.descripcion = descripcion;
        this.activo = activo;
        this.verificado = verificado;
    }

    public static PerfilPublicadorActualDTO desdePerfil(PerfilPublicador perfil) {
        Ciudad ciudad = perfil.getCiudadPrincipal();
        return new PerfilPublicadorActualDTO(
                perfil.getId(),
                perfil.getNombre(),
                perfil.getTipoPublicador(),
                perfil.getEstado(),
                ciudad != null ? ciudad.getId() : null,
                ciudad != null ? ciudad.getNombre() : null,
                perfil.getWhatsapp(),
                perfil.getInstagram(),
                perfil.getEmailContacto(),
                perfil.getTelefonoContacto(),
                perfil.getDescripcion(),
                perfil.getActivo(),
                perfil.getVerificado()
        );
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

    public String getEstado() {
        return estado;
    }

    public Long getCiudadPrincipalId() {
        return ciudadPrincipalId;
    }

    public String getCiudadPrincipalNombre() {
        return ciudadPrincipalNombre;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Boolean getActivo() {
        return activo;
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

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setCiudadPrincipalId(Long ciudadPrincipalId) {
        this.ciudadPrincipalId = ciudadPrincipalId;
    }

    public void setCiudadPrincipalNombre(String ciudadPrincipalNombre) {
        this.ciudadPrincipalNombre = ciudadPrincipalNombre;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setVerificado(Boolean verificado) {
        this.verificado = verificado;
    }
}
