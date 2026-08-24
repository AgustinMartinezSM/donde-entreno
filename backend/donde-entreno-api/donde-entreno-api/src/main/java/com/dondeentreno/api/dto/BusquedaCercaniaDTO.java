package com.dondeentreno.api.dto;

import java.util.List;

/**
 * Resultado del modo "cerca mío" (Fase 7).
 *
 * Lleva `sinCoordenadas` porque las actividades que todavía no tienen
 * punto cargado quedan FUERA de este modo —no se pueden ordenar por
 * distancia— y eso hay que decirlo en pantalla. La alternativa,
 * ubicarlas en el centro del barrio, inventa una precisión que no
 * existe y manda gente a la dirección equivocada.
 */
public class BusquedaCercaniaDTO {

    private List<ActividadDTO> contenido;
    private int radioKm;
    /** Cuántas quedaron fuera por no tener el punto cargado. */
    private long sinCoordenadas;
    /** Cuántas hay dentro del radio (el contenido puede venir cortado). */
    private long totalEnRadio;

    public BusquedaCercaniaDTO() {
    }

    public BusquedaCercaniaDTO(
            List<ActividadDTO> contenido,
            int radioKm,
            long sinCoordenadas,
            long totalEnRadio
    ) {
        this.contenido = contenido;
        this.radioKm = radioKm;
        this.sinCoordenadas = sinCoordenadas;
        this.totalEnRadio = totalEnRadio;
    }

    public List<ActividadDTO> getContenido() {
        return contenido;
    }

    public void setContenido(List<ActividadDTO> contenido) {
        this.contenido = contenido;
    }

    public int getRadioKm() {
        return radioKm;
    }

    public void setRadioKm(int radioKm) {
        this.radioKm = radioKm;
    }

    public long getSinCoordenadas() {
        return sinCoordenadas;
    }

    public void setSinCoordenadas(long sinCoordenadas) {
        this.sinCoordenadas = sinCoordenadas;
    }

    public long getTotalEnRadio() {
        return totalEnRadio;
    }

    public void setTotalEnRadio(long totalEnRadio) {
        this.totalEnRadio = totalEnRadio;
    }
}
