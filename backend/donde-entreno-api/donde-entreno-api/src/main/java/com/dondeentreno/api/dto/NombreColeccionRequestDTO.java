package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nombre de una coleccion de guardados (crear y renombrar, bloque 13).
 */
public class NombreColeccionRequestDTO {

    @NotBlank(message = "El nombre de la coleccion es obligatorio.")
    @Size(max = 60, message = "El nombre de la coleccion no puede superar los 60 caracteres.")
    private String nombre;

    public NombreColeccionRequestDTO() {
    }

    public NombreColeccionRequestDTO(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
