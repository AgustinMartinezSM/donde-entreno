package com.dondeentreno.api.service;

import java.math.BigDecimal;

/**
 * Distancia entre dos puntos por la fórmula de Haversine (Fase 7).
 *
 * En el service y no en la base a propósito: con el volumen actual (7
 * actividades publicadas) la distancia se calcula sobre el resultado
 * YA filtrado por ciudad y deporte, así que instalar PostGIS o un
 * índice geográfico sería ceremonia sin beneficio. Umbral anotado para
 * revisarlo: ~500 actividades.
 *
 * Haversine asume la Tierra esférica: el error es de ~0,3%, que a
 * escala de ciudad son centímetros. Alcanza de sobra para "está a
 * 1,2 km".
 */
public final class CalculadoraDistancia {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private CalculadoraDistancia() {
    }

    /**
     * @return distancia en kilómetros, o -1 si falta alguna coordenada
     *         (una actividad sin punto cargado no se puede ordenar por
     *         cercanía, y eso el llamador lo tiene que poder distinguir).
     */
    public static double kilometros(
            Double latitudOrigen,
            Double longitudOrigen,
            BigDecimal latitudDestino,
            BigDecimal longitudDestino
    ) {
        if (latitudOrigen == null || longitudOrigen == null
                || latitudDestino == null || longitudDestino == null) {
            return -1;
        }

        double latOrigenRad = Math.toRadians(latitudOrigen);
        double latDestinoRad = Math.toRadians(latitudDestino.doubleValue());
        double deltaLat = Math.toRadians(latitudDestino.doubleValue() - latitudOrigen);
        double deltaLon = Math.toRadians(longitudDestino.doubleValue() - longitudOrigen);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latOrigenRad) * Math.cos(latDestinoRad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        /* Un decimal: "a 1,2 km" es lo que se muestra; más precisión miente. */
        return Math.round(RADIO_TIERRA_KM * c * 10.0) / 10.0;
    }
}
