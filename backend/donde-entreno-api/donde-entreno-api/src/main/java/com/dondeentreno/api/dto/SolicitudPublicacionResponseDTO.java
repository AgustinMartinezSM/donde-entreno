package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para una solicitud publica de publicacion creada.
 */
public class SolicitudPublicacionResponseDTO {

    private Long id;
    private String codigoSeguimiento;
    private String estado;
    private OffsetDateTime createdAt;
    private String mensaje;

    public SolicitudPublicacionResponseDTO() {
    }

    public SolicitudPublicacionResponseDTO(
            Long id,
            String codigoSeguimiento,
            String estado,
            OffsetDateTime createdAt,
            String mensaje
    ) {
        this.id = id;
        this.codigoSeguimiento = codigoSeguimiento;
        this.estado = estado;
        this.createdAt = createdAt;
        this.mensaje = mensaje;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getMensaje() {
        return mensaje;
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

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
