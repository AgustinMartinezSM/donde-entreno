package com.dondeentreno.api.dto;

/**
 * Deporte del ranking de "lo más visto" (Fase 6). Sale del tracking
 * anónimo de la Fase 2: hasta ahora la home mostraba seis deportes
 * hardcodeados y los llamaba "populares".
 *
 * `vistas` viaja para poder ordenar y decidir en el cliente, no para
 * mostrarse: publicar el número exacto de vistas de cada deporte con
 * este volumen de tráfico diría más de la plataforma que del deporte.
 */
public class DeportePopularDTO {

    private String slug;
    private String nombre;
    private long vistas;

    public DeportePopularDTO() {
    }

    public DeportePopularDTO(String slug, String nombre, long vistas) {
        this.slug = slug;
        this.nombre = nombre;
        this.vistas = vistas;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getVistas() {
        return vistas;
    }

    public void setVistas(long vistas) {
        this.vistas = vistas;
    }
}
