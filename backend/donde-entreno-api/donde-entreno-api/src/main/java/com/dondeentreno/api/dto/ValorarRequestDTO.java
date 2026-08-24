package com.dondeentreno.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo del PUT de valoración (script 29).
 */
public class ValorarRequestDTO {

    @NotNull(message = "Falta el puntaje.")
    @Min(value = 1, message = "El puntaje minimo es 1.")
    @Max(value = 5, message = "El puntaje maximo es 5.")
    private Integer puntaje;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres.")
    private String comentario;

    private List<String> tags;

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
