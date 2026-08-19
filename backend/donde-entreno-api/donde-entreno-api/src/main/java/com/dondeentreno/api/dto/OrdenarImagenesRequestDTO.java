package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Orden manual de la galería de una actividad: la lista trae los ids de
 * TODAS las imágenes GALERIA activas de la actividad, en el orden en
 * que el publicador quiere verlas.
 */
public class OrdenarImagenesRequestDTO {

    @NotEmpty(message = "La lista de imagenes no puede estar vacia.")
    private List<Long> imagenIds;

    public List<Long> getImagenIds() {
        return imagenIds;
    }

    public void setImagenIds(List<Long> imagenIds) {
        this.imagenIds = imagenIds;
    }
}
