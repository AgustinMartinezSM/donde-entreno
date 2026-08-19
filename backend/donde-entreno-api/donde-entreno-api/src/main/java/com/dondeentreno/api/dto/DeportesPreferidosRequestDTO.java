package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request de PUT /api/usuario/deportes: el conjunto COMPLETO de slugs
 * elegidos (reemplazo total; la lista vacia significa "ninguno").
 */
public class DeportesPreferidosRequestDTO {

    @NotNull(message = "La lista de deportes es obligatoria.")
    @Size(max = 100, message = "La lista de deportes es demasiado larga.")
    private List<String> slugs;

    public DeportesPreferidosRequestDTO() {
    }

    public DeportesPreferidosRequestDTO(List<String> slugs) {
        this.slugs = slugs;
    }

    public List<String> getSlugs() {
        return slugs;
    }

    public void setSlugs(List<String> slugs) {
        this.slugs = slugs;
    }
}
