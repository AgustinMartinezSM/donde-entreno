package com.dondeentreno.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Campos propuestos para una solicitud de cambio de actividad.
 *
 * Todos son opcionales: un campo null (o en blanco) significa
 * "sin cambio propuesto". En V1 no se puede proponer vaciar un campo;
 * al menos uno debe venir con valor (regla validada en el service).
 */
public class SolicitudCambioActividadRequestDTO {

    @Size(max = 150, message = "El titulo no puede superar los 150 caracteres.")
    private String titulo;

    @Size(max = 5000, message = "La descripcion no puede superar los 5000 caracteres.")
    private String descripcion;

    @DecimalMin(value = "0", message = "El precio de referencia no puede ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "El precio de referencia no tiene un formato valido.")
    private BigDecimal precioReferencia;

    private Boolean mostrarPrecio;

    @Size(max = 30, message = "El whatsapp no puede superar los 30 caracteres.")
    private String whatsappContacto;

    @Size(max = 150, message = "El instagram no puede superar los 150 caracteres.")
    private String instagramContacto;

    @Email(message = "El email de contacto no tiene un formato valido.")
    @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres.")
    private String emailContacto;

    @Size(max = 50, message = "El nivel no puede superar los 50 caracteres.")
    private String nivel;

    @Size(max = 50, message = "La modalidad no puede superar los 50 caracteres.")
    private String modalidad;

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

    public String getWhatsappContacto() {
        return whatsappContacto;
    }

    public void setWhatsappContacto(String whatsappContacto) {
        this.whatsappContacto = whatsappContacto;
    }

    public String getInstagramContacto() {
        return instagramContacto;
    }

    public void setInstagramContacto(String instagramContacto) {
        this.instagramContacto = instagramContacto;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }
}
