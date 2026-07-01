package com.dondeentreno.api.dto;

/**
 * DTO de respuesta para la aprobacion admin de una solicitud de publicacion.
 */
public class SolicitudPublicacionAprobacionResponseDTO {

    private Long solicitudId;
    private String estado;
    private Long actividadId;
    private String actividadSlug;
    private String actividadTitulo;
    private String mensaje;

    public SolicitudPublicacionAprobacionResponseDTO() {
    }

    public SolicitudPublicacionAprobacionResponseDTO(
            Long solicitudId,
            String estado,
            Long actividadId,
            String actividadSlug,
            String actividadTitulo,
            String mensaje
    ) {
        this.solicitudId = solicitudId;
        this.estado = estado;
        this.actividadId = actividadId;
        this.actividadSlug = actividadSlug;
        this.actividadTitulo = actividadTitulo;
        this.mensaje = mensaje;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public String getEstado() {
        return estado;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public String getActividadTitulo() {
        return actividadTitulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setSolicitudId(Long solicitudId) {
        this.solicitudId = solicitudId;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }

    public void setActividadTitulo(String actividadTitulo) {
        this.actividadTitulo = actividadTitulo;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
