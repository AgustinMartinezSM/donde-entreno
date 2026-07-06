package com.dondeentreno.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request para registrar una cuenta publicadora y su perfil.
 */
public class RegistroPublicadorRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres.")
    private String apellido;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El email debe tener un formato valido.")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres.")
    private String email;

    @NotBlank(message = "La password es obligatoria.")
    private String password;

    @NotBlank(message = "La confirmacion de password es obligatoria.")
    private String confirmarPassword;

    @NotBlank(message = "El WhatsApp es obligatorio.")
    @Size(max = 30, message = "El WhatsApp no puede superar los 30 caracteres.")
    private String whatsapp;

    @NotBlank(message = "El tipo de publicador es obligatorio.")
    @Size(max = 50, message = "El tipo de publicador no puede superar los 50 caracteres.")
    @Pattern(
            regexp = "CLUB|GIMNASIO|PROFESOR_INDEPENDIENTE|INSTITUCION|ESCUELA_DEPORTIVA|ESPACIO_ENTRENAMIENTO",
            message = "El tipo de publicador informado no es valido."
    )
    private String tipoPublicador;

    @NotBlank(message = "El nombre publico es obligatorio.")
    @Size(max = 150, message = "El nombre publico no puede superar los 150 caracteres.")
    private String nombrePublico;

    @NotNull(message = "La ciudad principal es obligatoria.")
    @Positive(message = "La ciudad principal debe ser valida.")
    private Long ciudadPrincipalId;

    private String descripcion;

    @Size(max = 150, message = "El Instagram no puede superar los 150 caracteres.")
    private String instagram;

    @Email(message = "El email de contacto debe tener un formato valido.")
    @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres.")
    private String emailContacto;

    @Size(max = 30, message = "El telefono de contacto no puede superar los 30 caracteres.")
    private String telefonoContacto;

    public RegistroPublicadorRequestDTO() {
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public String getNombrePublico() {
        return nombrePublico;
    }

    public Long getCiudadPrincipalId() {
        return ciudadPrincipalId;
    }

    public String getDescripcion() {
        return descripcion;
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

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public void setNombrePublico(String nombrePublico) {
        this.nombrePublico = nombrePublico;
    }

    public void setCiudadPrincipalId(Long ciudadPrincipalId) {
        this.ciudadPrincipalId = ciudadPrincipalId;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
}
