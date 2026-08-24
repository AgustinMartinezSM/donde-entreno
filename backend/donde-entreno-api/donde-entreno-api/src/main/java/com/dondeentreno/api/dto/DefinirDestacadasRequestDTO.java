package com.dondeentreno.api.dto;

import java.util.List;

/**
 * Selección de actividades destacadas del publicador (Fase 5, script
 * 31). La lista viaja ORDENADA: la posición en el array es la posición
 * en el perfil. Vacía = limpiar las destacadas.
 *
 * El tope y la pertenencia se validan en el service contra la base,
 * nunca contra lo que mande el cliente.
 */
public class DefinirDestacadasRequestDTO {

    private List<Long> actividadIds;

    public DefinirDestacadasRequestDTO() {
    }

    public List<Long> getActividadIds() {
        return actividadIds != null ? actividadIds : List.of();
    }

    public void setActividadIds(List<Long> actividadIds) {
        this.actividadIds = actividadIds;
    }
}
