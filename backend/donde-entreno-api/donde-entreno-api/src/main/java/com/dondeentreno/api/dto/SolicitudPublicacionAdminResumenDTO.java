package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * DTO de resumen para listar solicitudes de publicacion en el panel admin.
 */
public class SolicitudPublicacionAdminResumenDTO {

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

    public SolicitudPublicacionAdminResumenDTO() {
    }

    public SolicitudPublicacionAdminResumenDTO(
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
            OffsetDateTime revisionFinalizadaAt
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
}
