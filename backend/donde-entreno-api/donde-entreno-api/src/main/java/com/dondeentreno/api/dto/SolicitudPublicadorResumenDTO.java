package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Resumen de solicitud visible para el panel publicador.
 */
public class SolicitudPublicadorResumenDTO {

    private Long id;
    private String codigoSeguimiento;
    private String estado;
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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime revisionIniciadaAt;
    private OffsetDateTime revisionFinalizadaAt;
    private String motivoRechazo;

    public SolicitudPublicadorResumenDTO() {
    }

    public SolicitudPublicadorResumenDTO(
            Long id,
            String codigoSeguimiento,
            String estado,
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
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime revisionIniciadaAt,
            OffsetDateTime revisionFinalizadaAt,
            String motivoRechazo
    ) {
        this.id = id;
        this.codigoSeguimiento = codigoSeguimiento;
        this.estado = estado;
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.revisionIniciadaAt = revisionIniciadaAt;
        this.revisionFinalizadaAt = revisionFinalizadaAt;
        this.motivoRechazo = motivoRechazo;
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

    public String getMotivoRechazo() {
        return motivoRechazo;
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

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}
