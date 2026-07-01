package com.dondeentreno.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de detalle para revisar solicitudes de publicacion en el panel admin.
 */
public class SolicitudPublicacionAdminDetalleDTO {

    private Long id;
    private String codigoSeguimiento;
    private String estado;
    private String origen;
    private String tipoPublicador;
    private String nombrePublicador;
    private String nombreActividad;
    private Long deporteId;
    private String deporteNombre;
    private String deporteOtro;
    private Long ciudadId;
    private String ciudadNombre;
    private String ciudadOtra;
    private Long barrioId;
    private String barrioNombre;
    private String barrioOtro;
    private String email;
    private String whatsapp;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime revisionIniciadaAt;
    private OffsetDateTime revisionFinalizadaAt;
    private String descripcion;
    private String nivel;
    private String enfoque;
    private String modalidad;
    private Integer edadMinima;
    private Integer edadMaxima;
    private BigDecimal precioReferencia;
    private Boolean mostrarPrecio;
    private String nombreLugar;
    private String direccion;
    private String referenciaUbicacion;
    private String instagram;
    private String observacionesSolicitante;
    private String motivoRechazo;
    private String observacionesRevision;
    private SolicitudPublicacionAdminRevisorDTO revisor;
    private Long actividadGeneradaId;
    private List<SolicitudPublicacionAdminHorarioDTO> horarios;

    public SolicitudPublicacionAdminDetalleDTO() {
    }

    public SolicitudPublicacionAdminDetalleDTO(
            Long id,
            String codigoSeguimiento,
            String estado,
            String origen,
            String tipoPublicador,
            String nombrePublicador,
            String nombreActividad,
            Long deporteId,
            String deporteNombre,
            String deporteOtro,
            Long ciudadId,
            String ciudadNombre,
            String ciudadOtra,
            Long barrioId,
            String barrioNombre,
            String barrioOtro,
            String email,
            String whatsapp,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime revisionIniciadaAt,
            OffsetDateTime revisionFinalizadaAt,
            String descripcion,
            String nivel,
            String enfoque,
            String modalidad,
            Integer edadMinima,
            Integer edadMaxima,
            BigDecimal precioReferencia,
            Boolean mostrarPrecio,
            String nombreLugar,
            String direccion,
            String referenciaUbicacion,
            String instagram,
            String observacionesSolicitante,
            String motivoRechazo,
            String observacionesRevision,
            SolicitudPublicacionAdminRevisorDTO revisor,
            Long actividadGeneradaId,
            List<SolicitudPublicacionAdminHorarioDTO> horarios
    ) {
        this.id = id;
        this.codigoSeguimiento = codigoSeguimiento;
        this.estado = estado;
        this.origen = origen;
        this.tipoPublicador = tipoPublicador;
        this.nombrePublicador = nombrePublicador;
        this.nombreActividad = nombreActividad;
        this.deporteId = deporteId;
        this.deporteNombre = deporteNombre;
        this.deporteOtro = deporteOtro;
        this.ciudadId = ciudadId;
        this.ciudadNombre = ciudadNombre;
        this.ciudadOtra = ciudadOtra;
        this.barrioId = barrioId;
        this.barrioNombre = barrioNombre;
        this.barrioOtro = barrioOtro;
        this.email = email;
        this.whatsapp = whatsapp;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.revisionIniciadaAt = revisionIniciadaAt;
        this.revisionFinalizadaAt = revisionFinalizadaAt;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.enfoque = enfoque;
        this.modalidad = modalidad;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
        this.precioReferencia = precioReferencia;
        this.mostrarPrecio = mostrarPrecio;
        this.nombreLugar = nombreLugar;
        this.direccion = direccion;
        this.referenciaUbicacion = referenciaUbicacion;
        this.instagram = instagram;
        this.observacionesSolicitante = observacionesSolicitante;
        this.motivoRechazo = motivoRechazo;
        this.observacionesRevision = observacionesRevision;
        this.revisor = revisor;
        this.actividadGeneradaId = actividadGeneradaId;
        this.horarios = horarios;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoSeguimiento() {
        return codigoSeguimiento;
    }

    public String getEstado() {
        return estado;
    }

    public String getOrigen() {
        return origen;
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

    public String getDeporteNombre() {
        return deporteNombre;
    }

    public String getDeporteOtro() {
        return deporteOtro;
    }

    public Long getCiudadId() {
        return ciudadId;
    }

    public String getCiudadNombre() {
        return ciudadNombre;
    }

    public String getCiudadOtra() {
        return ciudadOtra;
    }

    public Long getBarrioId() {
        return barrioId;
    }

    public String getBarrioNombre() {
        return barrioNombre;
    }

    public String getBarrioOtro() {
        return barrioOtro;
    }

    public String getEmail() {
        return email;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getRevisionIniciadaAt() {
        return revisionIniciadaAt;
    }

    public OffsetDateTime getRevisionFinalizadaAt() {
        return revisionFinalizadaAt;
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

    public String getNombreLugar() {
        return nombreLugar;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferenciaUbicacion() {
        return referenciaUbicacion;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getObservacionesSolicitante() {
        return observacionesSolicitante;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public String getObservacionesRevision() {
        return observacionesRevision;
    }

    public SolicitudPublicacionAdminRevisorDTO getRevisor() {
        return revisor;
    }

    public Long getActividadGeneradaId() {
        return actividadGeneradaId;
    }

    public List<SolicitudPublicacionAdminHorarioDTO> getHorarios() {
        return horarios;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigoSeguimiento(String codigoSeguimiento) {
        this.codigoSeguimiento = codigoSeguimiento;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
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

    public void setDeporteNombre(String deporteNombre) {
        this.deporteNombre = deporteNombre;
    }

    public void setDeporteOtro(String deporteOtro) {
        this.deporteOtro = deporteOtro;
    }

    public void setCiudadId(Long ciudadId) {
        this.ciudadId = ciudadId;
    }

    public void setCiudadNombre(String ciudadNombre) {
        this.ciudadNombre = ciudadNombre;
    }

    public void setCiudadOtra(String ciudadOtra) {
        this.ciudadOtra = ciudadOtra;
    }

    public void setBarrioId(Long barrioId) {
        this.barrioId = barrioId;
    }

    public void setBarrioNombre(String barrioNombre) {
        this.barrioNombre = barrioNombre;
    }

    public void setBarrioOtro(String barrioOtro) {
        this.barrioOtro = barrioOtro;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setRevisionIniciadaAt(OffsetDateTime revisionIniciadaAt) {
        this.revisionIniciadaAt = revisionIniciadaAt;
    }

    public void setRevisionFinalizadaAt(OffsetDateTime revisionFinalizadaAt) {
        this.revisionFinalizadaAt = revisionFinalizadaAt;
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

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setReferenciaUbicacion(String referenciaUbicacion) {
        this.referenciaUbicacion = referenciaUbicacion;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setObservacionesSolicitante(String observacionesSolicitante) {
        this.observacionesSolicitante = observacionesSolicitante;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public void setObservacionesRevision(String observacionesRevision) {
        this.observacionesRevision = observacionesRevision;
    }

    public void setRevisor(SolicitudPublicacionAdminRevisorDTO revisor) {
        this.revisor = revisor;
    }

    public void setActividadGeneradaId(Long actividadGeneradaId) {
        this.actividadGeneradaId = actividadGeneradaId;
    }

    public void setHorarios(List<SolicitudPublicacionAdminHorarioDTO> horarios) {
        this.horarios = horarios;
    }
}
