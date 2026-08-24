package com.dondeentreno.api.dto;

/**
 * Zona con actividad real (Fase 7): cuántas actividades publicadas hay
 * en cada barrio.
 *
 * Es el dato territorial que se puede dar HOY, sin una sola coordenada
 * cargada — el barrio ya viaja en cada actividad y el filtro por
 * barrio ya funciona de punta a punta.
 */
public class ZonaBarrioDTO {

    private Long barrioId;
    private String barrioNombre;
    private long cantidadActividades;

    public ZonaBarrioDTO() {
    }

    public ZonaBarrioDTO(Long barrioId, String barrioNombre, long cantidadActividades) {
        this.barrioId = barrioId;
        this.barrioNombre = barrioNombre;
        this.cantidadActividades = cantidadActividades;
    }

    public Long getBarrioId() {
        return barrioId;
    }

    public void setBarrioId(Long barrioId) {
        this.barrioId = barrioId;
    }

    public String getBarrioNombre() {
        return barrioNombre;
    }

    public void setBarrioNombre(String barrioNombre) {
        this.barrioNombre = barrioNombre;
    }

    public long getCantidadActividades() {
        return cantidadActividades;
    }

    public void setCantidadActividades(long cantidadActividades) {
        this.cantidadActividades = cantidadActividades;
    }
}
