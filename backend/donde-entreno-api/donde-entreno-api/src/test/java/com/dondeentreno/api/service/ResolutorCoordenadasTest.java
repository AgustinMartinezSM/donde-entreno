package com.dondeentreno.api.service;

import com.dondeentreno.api.exception.FiltroInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lectura del punto que pega el publicador (Fase 7). Es la pieza de
 * la que depende que las coordenadas existan, así que se prueba con
 * los formatos REALES que devuelve Google Maps.
 */
class ResolutorCoordenadasTest {

    private final ResolutorCoordenadas resolutor = new ResolutorCoordenadas();

    @Test
    void leeCoordenadasPegadasDirecto() {
        BigDecimal[] coordenadas = resolutor.resolver("-38.005477, -57.542611");

        assertEquals(new BigDecimal("-38.005477"), coordenadas[0]);
        assertEquals(new BigDecimal("-57.542611"), coordenadas[1]);
    }

    @Test
    void leeElLinkLargoDeGoogleMaps() {
        BigDecimal[] coordenadas = resolutor.resolver(
                "https://www.google.com/maps/place/Club/@-38.005477,-57.542611,17z/data=!3m1!4b1"
        );

        assertEquals(new BigDecimal("-38.005477"), coordenadas[0]);
        assertEquals(new BigDecimal("-57.542611"), coordenadas[1]);
    }

    @Test
    void leeElFormatoConParametroQ() {
        BigDecimal[] coordenadas = resolutor.resolver(
                "https://maps.google.com/?q=-38.012300,-57.542000"
        );

        assertEquals(new BigDecimal("-38.012300"), coordenadas[0]);
    }

    /**
     * El detalle que evita pines corridos: `@lat,lng` es el CENTRO DE
     * LA CÁMARA (se mueve si la persona arrastró el mapa antes de
     * copiar), mientras que `!3d!4d` es el punto del LUGAR. Con los
     * dos presentes tiene que ganar el del lugar.
     */
    @Test
    void conAmbosFormatosGanaElPuntoDelLugarYNoElDeLaCamara() {
        BigDecimal[] coordenadas = resolutor.resolver(
                "https://www.google.com/maps/place/Natatorio/@-38.100000,-57.600000,15z/"
                        + "data=!4m6!3m5!1s0x0:0x0!8m2!3d-38.034200!4d-57.557400"
        );

        assertEquals(new BigDecimal("-38.034200"), coordenadas[0]);
        assertEquals(new BigDecimal("-57.557400"), coordenadas[1]);
    }

    /**
     * El link corto no trae coordenadas y no se resuelve (habría que
     * seguir el redirect contra Google en el camino de guardado). El
     * mensaje tiene que EXPLICAR qué hacer, no ser genérico.
     */
    @Test
    void elLinkCortoExplicaQueHacer() {
        FiltroInvalidoException excepcion = assertThrows(
                FiltroInvalidoException.class,
                () -> resolutor.resolver("https://maps.app.goo.gl/AbCdEf123")
        );

        assertTrue(excepcion.getMessage().contains("link largo"),
                "El mensaje tiene que decir qué hacer con el link corto.");
    }

    @Test
    void loQueNoTieneCoordenadasDa400() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> resolutor.resolver("Av. Independencia 3030")
        );

        assertThrows(FiltroInvalidoException.class, () -> resolutor.resolver(""));
        assertThrows(FiltroInvalidoException.class, () -> resolutor.resolver(null));
    }

    @Test
    void unaCoordenadaFueraDeRangoDa400() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> resolutor.resolver("-95.000000, -57.542611")
        );
    }
}
