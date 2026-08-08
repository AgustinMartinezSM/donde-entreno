package com.dondeentreno.api.dto;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;

import java.time.OffsetDateTime;

/**
 * Imagen vista desde la cola de moderación del admin,
 * con los datos de la actividad dueña para dar contexto.
 */
public class ImagenAdminDTO {

    private Long id;
    private String url;
    private String tipoImagen;
    private String estadoModeracion;
    private String motivoRechazo;
    private Boolean activa;
    private OffsetDateTime createdAt;
    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;
    /*
      Una imagen cuelga de una actividad o de un perfil, nunca de las
      dos. Sin estos campos, el logo o la portada de un publicador
      llegaban a la cola sin nada que los identifique.
    */
    private Long perfilPublicadorId;
    private String perfilPublicadorNombre;

    public static ImagenAdminDTO desdeEntidad(Imagen imagen) {
        Actividad actividad = imagen.getActividad();
        PerfilPublicador perfil = imagen.getPerfilPublicador();

        ImagenAdminDTO dto = new ImagenAdminDTO();
        dto.setId(imagen.getId());
        dto.setUrl(imagen.getUrl());
        dto.setTipoImagen(imagen.getTipoImagen());
        dto.setEstadoModeracion(imagen.getEstadoModeracion());
        dto.setMotivoRechazo(imagen.getMotivoRechazo());
        dto.setActiva(imagen.getActiva());
        dto.setCreatedAt(imagen.getCreatedAt());
        dto.setActividadId(actividad != null ? actividad.getId() : null);
        dto.setActividadTitulo(actividad != null ? actividad.getTitulo() : null);
        dto.setActividadSlug(actividad != null ? actividad.getSlug() : null);
        dto.setPerfilPublicadorId(perfil != null ? perfil.getId() : null);
        dto.setPerfilPublicadorNombre(perfil != null ? perfil.getNombre() : null);
        return dto;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public String getEstadoModeracion() {
        return estadoModeracion;
    }

    public void setEstadoModeracion(String estadoModeracion) {
        this.estadoModeracion = estadoModeracion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
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
}
