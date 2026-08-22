package com.dondeentreno.api.dto;

/**
 * Señales agregadas de confianza del detalle público (etapa A del plan
 * de valoraciones): números anónimos, nunca nombres. El frontend solo
 * muestra las señales mayores que cero.
 */
public class SocialProofDTO {

    private long cantidadFavoritos;
    private long cantidadLikesFotos;
    private long cantidadPersonasEntrenaron30Dias;

    public SocialProofDTO() {
    }

    public SocialProofDTO(
            long cantidadFavoritos,
            long cantidadLikesFotos,
            long cantidadPersonasEntrenaron30Dias
    ) {
        this.cantidadFavoritos = cantidadFavoritos;
        this.cantidadLikesFotos = cantidadLikesFotos;
        this.cantidadPersonasEntrenaron30Dias = cantidadPersonasEntrenaron30Dias;
    }

    public long getCantidadFavoritos() {
        return cantidadFavoritos;
    }

    public void setCantidadFavoritos(long cantidadFavoritos) {
        this.cantidadFavoritos = cantidadFavoritos;
    }

    public long getCantidadLikesFotos() {
        return cantidadLikesFotos;
    }

    public void setCantidadLikesFotos(long cantidadLikesFotos) {
        this.cantidadLikesFotos = cantidadLikesFotos;
    }

    public long getCantidadPersonasEntrenaron30Dias() {
        return cantidadPersonasEntrenaron30Dias;
    }

    public void setCantidadPersonasEntrenaron30Dias(long cantidadPersonasEntrenaron30Dias) {
        this.cantidadPersonasEntrenaron30Dias = cantidadPersonasEntrenaron30Dias;
    }
}
