package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Un aviso del grupo con todo lo necesario para pintarlo: reacciones,
 * conteo de comentarios y, al abrirlo, los comentarios mismos.
 */
public class AvisoGrupoDTO {

    private Long id;
    private String texto;
    private OffsetDateTime createdAt;

    private Long imagenId;
    private String imagenUrl;

    private Long cantidadMeGusta;
    private Boolean meGusta;

    private Long cantidadComentarios;
    /** Solo al abrir el aviso. */
    private List<ComentarioAvisoDTO> comentarios;

    public AvisoGrupoDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Long getCantidadMeGusta() {
        return cantidadMeGusta;
    }

    public void setCantidadMeGusta(Long cantidadMeGusta) {
        this.cantidadMeGusta = cantidadMeGusta;
    }

    public Boolean getMeGusta() {
        return meGusta;
    }

    public void setMeGusta(Boolean meGusta) {
        this.meGusta = meGusta;
    }

    public Long getCantidadComentarios() {
        return cantidadComentarios;
    }

    public void setCantidadComentarios(Long cantidadComentarios) {
        this.cantidadComentarios = cantidadComentarios;
    }

    public List<ComentarioAvisoDTO> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<ComentarioAvisoDTO> comentarios) {
        this.comentarios = comentarios;
    }
}
