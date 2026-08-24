package com.dondeentreno.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Resumen público de valoraciones de una actividad (script 29).
 * `promedio` es null con menos de 3 valoraciones (regla del plan: un
 * 1 solitario no puede hundir a un club).
 */
public class ResumenValoracionesDTO {

    private Double promedio;
    private long cantidad;
    private Map<Integer, Long> distribucion;
    private List<ValoracionPublicaDTO> contenido;

    public ResumenValoracionesDTO() {
    }

    public ResumenValoracionesDTO(
            Double promedio,
            long cantidad,
            Map<Integer, Long> distribucion,
            List<ValoracionPublicaDTO> contenido
    ) {
        this.promedio = promedio;
        this.cantidad = cantidad;
        this.distribucion = distribucion;
        this.contenido = contenido;
    }

    public Double getPromedio() {
        return promedio;
    }

    public long getCantidad() {
        return cantidad;
    }

    public Map<Integer, Long> getDistribucion() {
        return distribucion;
    }

    public List<ValoracionPublicaDTO> getContenido() {
        return contenido;
    }
}
