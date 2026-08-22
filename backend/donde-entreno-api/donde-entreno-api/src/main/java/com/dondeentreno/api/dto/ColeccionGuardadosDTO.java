package com.dondeentreno.api.dto;

/**
 * Coleccion de guardados con su conteo (bloque 13).
 */
public class ColeccionGuardadosDTO {

    private Long id;
    private String nombre;
    private long cantidad;

    public ColeccionGuardadosDTO() {
    }

    public ColeccionGuardadosDTO(Long id, String nombre, long cantidad) {
        this.id = id;
        this.nombre = nombre;
        this.cantidad = cantidad;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public long getCantidad() {
        return cantidad;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCantidad(long cantidad) {
        this.cantidad = cantidad;
    }
}
