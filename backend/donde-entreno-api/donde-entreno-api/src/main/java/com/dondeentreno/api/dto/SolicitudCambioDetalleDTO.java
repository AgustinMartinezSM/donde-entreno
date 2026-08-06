package com.dondeentreno.api.dto;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.SolicitudCambioActividad;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detalle de una solicitud de cambio para el publicador y para el
 * admin. Incluye la comparacion campo a campo (solo los campos con
 * cambio propuesto).
 */
public class SolicitudCambioDetalleDTO {

    private Long id;
    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;
    private Long perfilPublicadorId;
    private String perfilPublicadorNombre;
    private String estado;
    private String motivoRechazo;
    private OffsetDateTime resueltoAt;
    private OffsetDateTime createdAt;
    private List<CampoCambioDTO> cambios;

    public static SolicitudCambioDetalleDTO desdeEntidad(
            SolicitudCambioActividad solicitud,
            List<CampoCambioDTO> cambios
    ) {
        Actividad actividad = solicitud.getActividad();

        SolicitudCambioDetalleDTO dto = new SolicitudCambioDetalleDTO();
        dto.setId(solicitud.getId());
        dto.setActividadId(actividad != null ? actividad.getId() : null);
        dto.setActividadTitulo(actividad != null ? actividad.getTitulo() : null);
        dto.setActividadSlug(actividad != null ? actividad.getSlug() : null);
        dto.setPerfilPublicadorId(
                solicitud.getPerfilPublicador() != null
                        ? solicitud.getPerfilPublicador().getId()
                        : null
        );
        dto.setPerfilPublicadorNombre(
                solicitud.getPerfilPublicador() != null
                        ? solicitud.getPerfilPublicador().getNombre()
                        : null
        );
        dto.setEstado(solicitud.getEstado());
        dto.setMotivoRechazo(solicitud.getMotivoRechazo());
        dto.setResueltoAt(solicitud.getResueltoAt());
        dto.setCreatedAt(solicitud.getCreatedAt());
        dto.setCambios(cambios);
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public String getActividadTitulo() {
        return actividadTitulo;
    }

    public void setActividadTitulo(String actividadTitulo) {
        this.actividadTitulo = actividadTitulo;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public String getPerfilPublicadorNombre() {
        return perfilPublicadorNombre;
    }

    public void setPerfilPublicadorNombre(String perfilPublicadorNombre) {
        this.perfilPublicadorNombre = perfilPublicadorNombre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public OffsetDateTime getResueltoAt() {
        return resueltoAt;
    }

    public void setResueltoAt(OffsetDateTime resueltoAt) {
        this.resueltoAt = resueltoAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CampoCambioDTO> getCambios() {
        return cambios;
    }

    public void setCambios(List<CampoCambioDTO> cambios) {
        this.cambios = cambios;
    }
}
