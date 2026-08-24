package com.dondeentreno.api.service;

import com.dondeentreno.api.exception.FiltroInvalidoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte lo que el publicador PEGA en un par de coordenadas
 * (Fase 7).
 *
 * La decisión de producto es no geocodificar direcciones: el geocoding
 * gratuito de direcciones argentinas es impreciso, y un pin mal puesto
 * manda gente al lugar equivocado — peor que no tener pin. En cambio,
 * el link de Google Maps que la persona ya sabe compartir trae el
 * punto EXACTO que ella misma eligió.
 *
 * Formatos aceptados:
 * - Coordenadas sueltas: "-38.005477, -57.542611"
 * - Link con @lat,lng:   ".../maps/place/Club/@-38.005477,-57.542611,17z/..."
 * - Link con ?q=lat,lng: "https://maps.google.com/?q=-38.005477,-57.542611"
 * - Link con !3dlat!4dlng (el formato interno de los links largos)
 *
 * NO resuelve los links cortos (maps.app.goo.gl): habría que seguir el
 * redirect contra Google desde el backend, y eso es una llamada
 * saliente en el camino de guardado. El mensaje de error explica qué
 * hacer, que es más honesto que fallar en silencio.
 */
@Component
public class ResolutorCoordenadas {

    /* Precisión de la columna: NUMERIC(9,6) → 6 decimales, ~11 cm. */
    private static final int DECIMALES = 6;

    private static final Pattern ARROBA =
            Pattern.compile("@(-?\\d{1,3}\\.\\d+),(-?\\d{1,3}\\.\\d+)");
    private static final Pattern PARAMETRO_Q =
            Pattern.compile("[?&]q=(-?\\d{1,3}\\.\\d+),\\s*(-?\\d{1,3}\\.\\d+)");
    private static final Pattern FORMATO_3D_4D =
            Pattern.compile("!3d(-?\\d{1,3}\\.\\d+)!4d(-?\\d{1,3}\\.\\d+)");
    private static final Pattern SUELTAS =
            Pattern.compile("^\\s*(-?\\d{1,3}\\.\\d+)\\s*,\\s*(-?\\d{1,3}\\.\\d+)\\s*$");

    private static final Pattern LINK_CORTO =
            Pattern.compile("(maps\\.app\\.goo\\.gl|goo\\.gl/maps)", Pattern.CASE_INSENSITIVE);

    /**
     * @param pegado lo que la persona pegó (link o coordenadas).
     * @return [latitud, longitud] validadas.
     * @throws FiltroInvalidoException con un mensaje que explica qué
     *         hacer, nunca un error genérico.
     */
    public BigDecimal[] resolver(String pegado) {
        String texto = pegado != null ? pegado.trim() : "";

        if (texto.isEmpty()) {
            throw new FiltroInvalidoException(
                    "Pegá el link de Google Maps de tu sede o las coordenadas."
            );
        }

        /*
          El orden importa: el formato interno (!3d!4d) es el PUNTO del
          lugar, mientras que @lat,lng es el CENTRO DE LA CÁMARA, que
          puede estar corrido si la persona movió el mapa antes de
          copiar. Con los dos presentes, gana el del lugar.
        */
        BigDecimal[] coordenadas = primerMatch(FORMATO_3D_4D, texto);

        if (coordenadas == null) {
            coordenadas = primerMatch(SUELTAS, texto);
        }
        if (coordenadas == null) {
            coordenadas = primerMatch(PARAMETRO_Q, texto);
        }
        if (coordenadas == null) {
            coordenadas = primerMatch(ARROBA, texto);
        }

        if (coordenadas == null) {
            if (LINK_CORTO.matcher(texto).find()) {
                throw new FiltroInvalidoException(
                        "Ese es un link corto y no trae las coordenadas. Abrilo en el "
                                + "navegador y pegá el link largo que queda en la barra de "
                                + "direcciones, o pegá directamente las coordenadas."
                );
            }

            throw new FiltroInvalidoException(
                    "No pudimos leer las coordenadas. Pegá el link de Google Maps de tu "
                            + "sede (el que aparece en la barra del navegador) o las "
                            + "coordenadas separadas por coma."
            );
        }

        validarRango(coordenadas[0], coordenadas[1]);

        return coordenadas;
    }

    private BigDecimal[] primerMatch(Pattern patron, String texto) {
        Matcher matcher = patron.matcher(texto);

        if (!matcher.find()) {
            return null;
        }

        try {
            return new BigDecimal[]{
                    new BigDecimal(matcher.group(1)).setScale(DECIMALES, RoundingMode.HALF_UP),
                    new BigDecimal(matcher.group(2)).setScale(DECIMALES, RoundingMode.HALF_UP)
            };
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    /* El mismo rango que el CHECK de la base: fallar acá da mejor mensaje. */
    private void validarRango(BigDecimal latitud, BigDecimal longitud) {
        if (latitud.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitud.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new FiltroInvalidoException("La latitud está fuera de rango.");
        }

        if (longitud.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitud.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new FiltroInvalidoException("La longitud está fuera de rango.");
        }
    }
}
