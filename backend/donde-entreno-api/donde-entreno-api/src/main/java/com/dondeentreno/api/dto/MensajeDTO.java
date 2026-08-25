package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;

/**
 * Un mensaje del hilo.
 *
 * NO viaja `leidoAt`: se guarda para el contador, pero mostrar "visto
 * a las 14:32" crea una expectativa de respuesta inmediata que un club
 * no puede cumplir (decisión del plan).
 */
public class MensajeDTO {

    private Long id;
    private String texto;
    private OffsetDateTime createdAt;
    /** Si lo escribió quien está mirando: alcanza para alinear la burbuja. */
    private Boolean esPropio;
    /** Un mensaje ocultado por el admin deja el hueco, no desaparece. */
    private Boolean oculto;

    public MensajeDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getEsPropio() {
        return esPropio;
    }

    public void setEsPropio(Boolean esPropio) {
        this.esPropio = esPropio;
    }

    public Boolean getOculto() {
        return oculto;
    }

    public void setOculto(Boolean oculto) {
        this.oculto = oculto;
    }
}
