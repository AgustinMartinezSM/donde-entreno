package com.dondeentreno.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Campos del perfil publicador que se pueden editar en forma directa
 * (sin revision admin): descripcion, instagram y email de contacto.
 *
 * Los campos sensibles (nombre publico, tipo de publicador, ciudad
 * principal, whatsapp/telefono y estado) quedan fuera a proposito:
 * van a editarse mediante un flujo con revision admin.
 *
 * Semantica PATCH:
 * - campo null: no se modifica.
 * - campo vacio o solo espacios: se limpia (queda null en el perfil).
 */
public class ActualizarPerfilPublicadorRequestDTO {

    @Size(max = 2000, message = "La descripcion no puede superar los 2000 caracteres.")
    private String descripcion;

    @Size(max = 150, message = "El instagram no puede superar los 150 caracteres.")
    private String instagram;

    @Email(message = "El email de contacto no tiene un formato valido.")
    @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres.")
    private String emailContacto;

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }
}
