package com.dondeentreno.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Campos del perfil publicador que se pueden editar en forma directa
 * (sin revision admin): nombre publico, descripcion, instagram y email
 * de contacto.
 *
 * El nombre publico se sumo en fase 5e con edicion DIRECTA (decision de
 * Agustin 2026-08-21): la descripcion —texto libre mas largo y con el
 * mismo riesgo— ya era directa, y la identidad real tiene su propia
 * herramienta (el badge de verificado). Tipo de publicador, ciudad
 * principal, whatsapp/telefono y estado siguen fuera a proposito.
 *
 * Semantica PATCH:
 * - campo null: no se modifica.
 * - campo vacio o solo espacios: se limpia (queda null en el perfil).
 * - EXCEPCION nombre: es obligatorio en el schema, vacio = 400.
 */
public class ActualizarPerfilPublicadorRequestDTO {

    @Size(max = 150, message = "El nombre publico no puede superar los 150 caracteres.")
    private String nombre;

    @Size(max = 2000, message = "La descripcion no puede superar los 2000 caracteres.")
    private String descripcion;

    @Size(max = 150, message = "El instagram no puede superar los 150 caracteres.")
    private String instagram;

    @Email(message = "El email de contacto no tiene un formato valido.")
    @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres.")
    private String emailContacto;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

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
