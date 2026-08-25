package com.dondeentreno.api.dto;

import java.util.List;

/**
 * El grupo de una actividad, tal como lo ve un miembro.
 *
 * `esMiembro` viaja siempre; `avisos` solo si lo es. Asi la misma
 * respuesta sirve para pintar el boton "Sumarme al grupo" sin filtrar
 * contenido en el frontend.
 */
public class GrupoActividadDTO {

    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;

    private Boolean esMiembro;
    private Long cantidadMiembros;

    private List<AvisoGrupoDTO> avisos;

    public GrupoActividadDTO() {
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

    public Boolean getEsMiembro() {
        return esMiembro;
    }

    public void setEsMiembro(Boolean esMiembro) {
        this.esMiembro = esMiembro;
    }

    public Long getCantidadMiembros() {
        return cantidadMiembros;
    }

    public void setCantidadMiembros(Long cantidadMiembros) {
        this.cantidadMiembros = cantidadMiembros;
    }

    public List<AvisoGrupoDTO> getAvisos() {
        return avisos;
    }

    public void setAvisos(List<AvisoGrupoDTO> avisos) {
        this.avisos = avisos;
    }
}
