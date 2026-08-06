package com.dondeentreno.api.dto;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.SolicitudCambioActividad;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resumen de una solicitud de cambio para listados
 * (panel publicador y cola admin).
 */
public class SolicitudCambioResumenDTO {

    private Long id;
    private Long actividadId;
    private String actividadTitulo;
    private String estado;
    private List<String> camposPropuestos;
    private OffsetDateTime createdAt;

    public static SolicitudCambioResumenDTO desdeEntidad(
            SolicitudCambioActividad solicitud,
            List<String> camposPropuestos
    ) {
        Actividad actividad = solicitud.getActividad();

        SolicitudCambioResumenDTO dto = new SolicitudCambioResumenDTO();
        dto.setId(solicitud.getId());
        dto.setActividadId(actividad != null ? actividad.getId() : null);
        dto.setActividadTitulo(actividad != null ? actividad.getTitulo() : null);
        dto.setEstado(solicitud.getEstado());
        dto.setCamposPropuestos(camposPropuestos);
        dto.setCreatedAt(solicitud.getCreatedAt());
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<String> getCamposPropuestos() {
        return camposPropuestos;
    }

    public void setCamposPropuestos(List<String> camposPropuestos) {
        this.camposPropuestos = camposPropuestos;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
