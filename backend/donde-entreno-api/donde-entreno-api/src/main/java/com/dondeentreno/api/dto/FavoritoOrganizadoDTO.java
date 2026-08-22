package com.dondeentreno.api.dto;

/**
 * Un guardado con su organizacion (bloque 13): la card publica mas la
 * coleccion y la nota del usuario. Endpoint nuevo y aditivo — el GET
 * plano de favoritos no cambia de forma.
 */
public class FavoritoOrganizadoDTO {

    private ActividadDTO actividad;
    private Long coleccionId;
    private String nota;

    public FavoritoOrganizadoDTO() {
    }

    public FavoritoOrganizadoDTO(ActividadDTO actividad, Long coleccionId, String nota) {
        this.actividad = actividad;
        this.coleccionId = coleccionId;
        this.nota = nota;
    }

    public ActividadDTO getActividad() {
        return actividad;
    }

    public Long getColeccionId() {
        return coleccionId;
    }

    public String getNota() {
        return nota;
    }

    public void setActividad(ActividadDTO actividad) {
        this.actividad = actividad;
    }

    public void setColeccionId(Long coleccionId) {
        this.coleccionId = coleccionId;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
