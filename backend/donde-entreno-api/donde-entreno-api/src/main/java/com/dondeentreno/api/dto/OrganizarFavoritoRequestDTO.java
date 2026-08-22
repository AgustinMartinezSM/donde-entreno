package com.dondeentreno.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Organizacion de un guardado (bloque 13): a que coleccion pertenece y
 * su nota personal.
 *
 * Semantica de REEMPLAZO TOTAL a proposito (no PATCH por campo): la UI
 * siempre conoce los dos valores y manda el estado deseado completo.
 * coleccionId null = "Todos"; nota null o vacia = sin nota.
 */
public class OrganizarFavoritoRequestDTO {

    private Long coleccionId;

    @Size(max = 280, message = "La nota no puede superar los 280 caracteres.")
    private String nota;

    public OrganizarFavoritoRequestDTO() {
    }

    public OrganizarFavoritoRequestDTO(Long coleccionId, String nota) {
        this.coleccionId = coleccionId;
        this.nota = nota;
    }

    public Long getColeccionId() {
        return coleccionId;
    }

    public String getNota() {
        return nota;
    }

    public void setColeccionId(Long coleccionId) {
        this.coleccionId = coleccionId;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
