package com.dondeentreno.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

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

    // ==========================================================
    // Campos nuevos (script 24). Null = sin cambio propuesto.
    // ==========================================================

    private Long deporteId;

    @Min(value = 0, message = "La edad minima no puede ser negativa.")
    @Max(value = 120, message = "La edad minima no puede superar 120.")
    private Integer edadMinima;

    @Min(value = 0, message = "La edad maxima no puede ser negativa.")
    @Max(value = 120, message = "La edad maxima no puede superar 120.")
    private Integer edadMaxima;

    @Size(max = 50, message = "El enfoque no puede superar los 50 caracteres.")
    private String enfoque;

    @Size(max = 150, message = "El nombre de la sede no puede superar los 150 caracteres.")
    private String ubicacionNombre;

    @Size(max = 255, message = "La direccion no puede superar los 255 caracteres.")
    private String ubicacionDireccion;

    @Size(max = 255, message = "La referencia no puede superar los 255 caracteres.")
    private String ubicacionReferencia;

    private Long ubicacionBarrioId;

    /** true = reemplazar el conjunto de horarios por `horarios` (>=1). */
    private Boolean cambiaHorarios;

    @Valid
    private List<SolicitudPublicacionHorarioRequestDTO> horarios;

    public Long getDeporteId() {
        return deporteId;
    }

    public void setDeporteId(Long deporteId) {
        this.deporteId = deporteId;
    }

    public Integer getEdadMinima() {
        return edadMinima;
    }

    public void setEdadMinima(Integer edadMinima) {
        this.edadMinima = edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public void setEdadMaxima(Integer edadMaxima) {
        this.edadMaxima = edadMaxima;
    }

    public String getEnfoque() {
        return enfoque;
    }

    public void setEnfoque(String enfoque) {
        this.enfoque = enfoque;
    }

    public String getUbicacionNombre() {
        return ubicacionNombre;
    }

    public void setUbicacionNombre(String ubicacionNombre) {
        this.ubicacionNombre = ubicacionNombre;
    }

    public String getUbicacionDireccion() {
        return ubicacionDireccion;
    }

    public void setUbicacionDireccion(String ubicacionDireccion) {
        this.ubicacionDireccion = ubicacionDireccion;
    }

    public String getUbicacionReferencia() {
        return ubicacionReferencia;
    }

    public void setUbicacionReferencia(String ubicacionReferencia) {
        this.ubicacionReferencia = ubicacionReferencia;
    }

    public Long getUbicacionBarrioId() {
        return ubicacionBarrioId;
    }

    public void setUbicacionBarrioId(Long ubicacionBarrioId) {
        this.ubicacionBarrioId = ubicacionBarrioId;
    }

    public Boolean getCambiaHorarios() {
        return cambiaHorarios;
    }

    public void setCambiaHorarios(Boolean cambiaHorarios) {
        this.cambiaHorarios = cambiaHorarios;
    }

    public List<SolicitudPublicacionHorarioRequestDTO> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<SolicitudPublicacionHorarioRequestDTO> horarios) {
        this.horarios = horarios;
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
