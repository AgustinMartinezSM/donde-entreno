package com.dondeentreno.api.dto;

/**
 * Un campo con cambio propuesto: valor actual vs valor propuesto.
 * El backend arma la comparacion; el frontend solo la pinta.
 */
public class CampoCambioDTO {

    private String campo;
    private String valorActual;
    private String valorPropuesto;

    public CampoCambioDTO() {
    }

    public CampoCambioDTO(String campo, String valorActual, String valorPropuesto) {
        this.campo = campo;
        this.valorActual = valorActual;
        this.valorPropuesto = valorPropuesto;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorActual() {
        return valorActual;
    }

    public void setValorActual(String valorActual) {
        this.valorActual = valorActual;
    }

    public String getValorPropuesto() {
        return valorPropuesto;
    }

    public void setValorPropuesto(String valorPropuesto) {
        this.valorPropuesto = valorPropuesto;
    }
}
