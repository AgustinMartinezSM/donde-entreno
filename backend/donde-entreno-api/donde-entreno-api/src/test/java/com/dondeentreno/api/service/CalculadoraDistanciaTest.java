package com.dondeentreno.api.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Distancia Haversine (Fase 7), probada con puntos REALES de Mar del
 * Plata para que el número sea verificable en un mapa y no solo
 * "consistente consigo mismo".
 */
class CalculadoraDistanciaTest {

    /* Sede Kimberley (Constitución) y Natatorio, del seed. */
    private static final double LAT_KIMBERLEY = -38.005477;
    private static final double LNG_KIMBERLEY = -57.542611;

    @Test
    void elMismoPuntoDaCero() {
        double km = CalculadoraDistancia.kilometros(
                LAT_KIMBERLEY,
                LNG_KIMBERLEY,
                BigDecimal.valueOf(LAT_KIMBERLEY),
                BigDecimal.valueOf(LNG_KIMBERLEY)
        );

        assertEquals(0.0, km);
    }

    /**
     * Kimberley → Natatorio: ~3,4 km en línea recta (verificable en
     * un mapa). Se acepta un margen chico porque Haversine asume la
     * Tierra esférica.
     */
    @Test
    void dosPuntosDeMarDelPlataDanLaDistanciaEsperada() {
        double km = CalculadoraDistancia.kilometros(
                LAT_KIMBERLEY,
                LNG_KIMBERLEY,
                BigDecimal.valueOf(-38.034200),
                BigDecimal.valueOf(-57.557400)
        );

        assertTrue(km > 3.0 && km < 4.0,
                "Esperaba ~3,4 km entre esos dos puntos, dio " + km);
    }

    /**
     * Sin coordenadas devuelve -1 y NO 0: un 0 se ordenaría primero,
     * o sea que las actividades sin punto cargado aparecerían como
     * "las más cercanas". El llamador tiene que poder distinguirlas.
     */
    @Test
    void sinCoordenadasDevuelveMenosUnoYNoCero() {
        assertEquals(-1, CalculadoraDistancia.kilometros(
                LAT_KIMBERLEY, LNG_KIMBERLEY, null, null));
        assertEquals(-1, CalculadoraDistancia.kilometros(
                LAT_KIMBERLEY, LNG_KIMBERLEY, BigDecimal.valueOf(-38.0), null));
        assertEquals(-1, CalculadoraDistancia.kilometros(
                null, null, BigDecimal.valueOf(-38.0), BigDecimal.valueOf(-57.5)));
    }

    @Test
    void elResultadoVieneRedondeadoAUnDecimal() {
        double km = CalculadoraDistancia.kilometros(
                LAT_KIMBERLEY,
                LNG_KIMBERLEY,
                BigDecimal.valueOf(-38.012300),
                BigDecimal.valueOf(-57.542000)
        );

        assertEquals(km, Math.round(km * 10.0) / 10.0);
    }
}
