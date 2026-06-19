package com.dondeentreno.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para crear una solicitud publica de publicacion.
 */
public class SolicitudPublicacionRequestDTO {

    @NotBlank(message = "El tipo de publicador es obligatorio.")
    @Size(max = 50, message = "El tipo de publicador no puede superar los 50 caracteres.")
    @Pattern(
            regexp = "CLUB|GIMNASIO|PROFESOR_INDEPENDIENTE|INSTITUCION|ESCUELA_DEPORTIVA|ESPACIO_ENTRENAMIENTO",
            message = "El tipo de publicador informado no es valido."
    )
    private String tipoPublicador;

    @NotBlank(message = "El nombre del publicador es obligatorio.")
    @Size(max = 150, message = "El nombre del publicador no puede superar los 150 caracteres.")
    private String nombrePublicador;

    @NotBlank(message = "El nombre de la actividad es obligatorio.")
    @Size(max = 150, message = "El nombre de la actividad no puede superar los 150 caracteres.")
    private String nombreActividad;

    @Positive(message = "El deporte seleccionado debe ser valido.")
    private Long deporteId;

    @Size(max = 100, message = "El deporte informado no puede superar los 100 caracteres.")
    private String deporteOtro;

    @NotBlank(message = "La descripcion es obligatoria.")
    private String descripcion;

    @NotBlank(message = "El nivel es obligatorio.")
    @Size(max = 50, message = "El nivel no puede superar los 50 caracteres.")
    @Pattern(
            regexp = "PRINCIPIANTE|INTERMEDIO|AVANZADO|TODOS",
            message = "El nivel informado no es valido."
    )
    private String nivel;

    @NotBlank(message = "El enfoque es obligatorio.")
    @Size(max = 50, message = "El enfoque no puede superar los 50 caracteres.")
    @Pattern(
            regexp = "RECREATIVO|COMPETITIVO|MIXTO",
            message = "El enfoque informado no es valido."
    )
    private String enfoque;

    @NotBlank(message = "La modalidad es obligatoria.")
    @Size(max = 50, message = "La modalidad no puede superar los 50 caracteres.")
    @Pattern(
            regexp = "PRESENCIAL|MIXTA",
            message = "La modalidad informada no es valida."
    )
    private String modalidad;

    @PositiveOrZero(message = "La edad minima no puede ser negativa.")
    private Integer edadMinima;

    @PositiveOrZero(message = "La edad maxima no puede ser negativa.")
    private Integer edadMaxima;

    @DecimalMin(value = "0.00", message = "El precio de referencia no puede ser negativo.")
    @Digits(
            integer = 8,
            fraction = 2,
            message = "El precio de referencia debe tener como maximo 8 enteros y 2 decimales."
    )
    private BigDecimal precioReferencia;

    private Boolean mostrarPrecio;

    @Positive(message = "La ciudad seleccionada debe ser valida.")
    private Long ciudadId;

    @Size(max = 100, message = "La ciudad informada no puede superar los 100 caracteres.")
    private String ciudadOtra;

    @Positive(message = "El barrio seleccionado debe ser valido.")
    private Long barrioId;

    @Size(max = 100, message = "El barrio informado no puede superar los 100 caracteres.")
    private String barrioOtro;

    @Size(max = 150, message = "El nombre del lugar no puede superar los 150 caracteres.")
    private String nombreLugar;

    @Size(max = 255, message = "La direccion no puede superar los 255 caracteres.")
    private String direccion;

    @Size(max = 255, message = "La referencia de ubicacion no puede superar los 255 caracteres.")
    private String referenciaUbicacion;

    @Size(max = 40, message = "El WhatsApp no puede superar los 40 caracteres.")
    private String whatsapp;

    @Size(max = 150, message = "El Instagram no puede superar los 150 caracteres.")
    private String instagram;

    @Email(message = "El email informado no es valido.")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres.")
    private String email;

    private String observacionesSolicitante;

    @NotNull(message = "Debe aceptar las condiciones.")
    @AssertTrue(message = "Debe aceptar las condiciones.")
    private Boolean aceptaCondiciones;

    @Valid
    @NotEmpty(message = "Debe incluir al menos un horario.")
    private List<SolicitudPublicacionHorarioRequestDTO> horarios;

    public SolicitudPublicacionRequestDTO() {
    }

    public SolicitudPublicacionRequestDTO(
            String tipoPublicador,
            String nombrePublicador,
            String nombreActividad,
            Long deporteId,
            String deporteOtro,
            String descripcion,
            String nivel,
            String enfoque,
            String modalidad,
            Integer edadMinima,
            Integer edadMaxima,
            BigDecimal precioReferencia,
            Boolean mostrarPrecio,
            Long ciudadId,
            String ciudadOtra,
            Long barrioId,
            String barrioOtro,
            String nombreLugar,
            String direccion,
            String referenciaUbicacion,
            String whatsapp,
            String instagram,
            String email,
            String observacionesSolicitante,
            Boolean aceptaCondiciones,
            List<SolicitudPublicacionHorarioRequestDTO> horarios
    ) {
        this.tipoPublicador = tipoPublicador;
        this.nombrePublicador = nombrePublicador;
        this.nombreActividad = nombreActividad;
        this.deporteId = deporteId;
        this.deporteOtro = deporteOtro;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.enfoque = enfoque;
        this.modalidad = modalidad;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
        this.precioReferencia = precioReferencia;
        this.mostrarPrecio = mostrarPrecio;
        this.ciudadId = ciudadId;
        this.ciudadOtra = ciudadOtra;
        this.barrioId = barrioId;
        this.barrioOtro = barrioOtro;
        this.nombreLugar = nombreLugar;
        this.direccion = direccion;
        this.referenciaUbicacion = referenciaUbicacion;
        this.whatsapp = whatsapp;
        this.instagram = instagram;
        this.email = email;
        this.observacionesSolicitante = observacionesSolicitante;
        this.aceptaCondiciones = aceptaCondiciones;
        this.horarios = horarios;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public String getNombrePublicador() {
        return nombrePublicador;
    }

    public String getNombreActividad() {
        return nombreActividad;
    }

    public Long getDeporteId() {
        return deporteId;
    }

    public String getDeporteOtro() {
        return deporteOtro;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getNivel() {
        return nivel;
    }

    public String getEnfoque() {
        return enfoque;
    }

    public String getModalidad() {
        return modalidad;
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

    public Long getCiudadId() {
        return ciudadId;
    }

    public String getCiudadOtra() {
        return ciudadOtra;
    }

    public Long getBarrioId() {
        return barrioId;
    }

    public String getBarrioOtro() {
        return barrioOtro;
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

    public String getObservacionesSolicitante() {
        return observacionesSolicitante;
    }

    public Boolean getAceptaCondiciones() {
        return aceptaCondiciones;
    }

    public List<SolicitudPublicacionHorarioRequestDTO> getHorarios() {
        return horarios;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public void setNombrePublicador(String nombrePublicador) {
        this.nombrePublicador = nombrePublicador;
    }

    public void setNombreActividad(String nombreActividad) {
        this.nombreActividad = nombreActividad;
    }

    public void setDeporteId(Long deporteId) {
        this.deporteId = deporteId;
    }

    public void setDeporteOtro(String deporteOtro) {
        this.deporteOtro = deporteOtro;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public void setEnfoque(String enfoque) {
        this.enfoque = enfoque;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
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

    public void setCiudadId(Long ciudadId) {
        this.ciudadId = ciudadId;
    }

    public void setCiudadOtra(String ciudadOtra) {
        this.ciudadOtra = ciudadOtra;
    }

    public void setBarrioId(Long barrioId) {
        this.barrioId = barrioId;
    }

    public void setBarrioOtro(String barrioOtro) {
        this.barrioOtro = barrioOtro;
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

    public void setObservacionesSolicitante(String observacionesSolicitante) {
        this.observacionesSolicitante = observacionesSolicitante;
    }

    public void setAceptaCondiciones(Boolean aceptaCondiciones) {
        this.aceptaCondiciones = aceptaCondiciones;
    }

    public void setHorarios(List<SolicitudPublicacionHorarioRequestDTO> horarios) {
        this.horarios = horarios;
    }
}
