package com.dondeentreno.api.asistente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.stream.Stream;

/**
 * Lo que devuelve el modelo. Todo campo de acá es una PROPUESTA, no un
 * hecho: nada se muestra sin pasar antes por validación.
 *
 * Cambio de fondo respecto del asistente V1, donde el equivalente
 * (InterpretacionRemota) traía cinco términos y ni una palabra de texto.
 * Ahora el modelo escribe, y por eso cada campo tiene su propio candado:
 *
 * - {@code mensaje} pasa por SanitizadorTexto (se le sacan enlaces,
 *   precios, horarios y datos de contacto) y tiene prohibido por
 *   instrucción afirmar qué hay publicado.
 * - {@code deportes} pasa por RecomendadorDeportes.validar: lo que no
 *   existe se cae solo y lo rechazado se descarta aunque el modelo insista.
 * - {@code filtros} vuelve a pasar por ResolutorConsulta contra el catálogo
 *   real, igual que en V1.
 * - No hay campo de enlaces, a propósito. El modelo no propone destinos:
 *   los arma el backend con slugs de la base.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaModelo(
        String tipoRespuesta,
        String mensaje,
        List<DeportePropuesto> deportes,
        FiltrosPropuestos filtros,
        String preguntaSeguimiento
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeportePropuesto(String nombre, String motivo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiltrosPropuestos(
            String deporte,
            String categoria,
            String barrio,
            String nivel,
            String modalidad
    ) {

        /**
         * Junta los términos en una frase para volver a pasarla por el
         * mismo resolutor determinístico que usa el camino local. Si el
         * modelo inventó algo que no está en el catálogo, no matchea y se
         * descarta solo: no hace falta una lista negra.
         */
        public String comoFrase() {
            return Stream.of(deporte, categoria, barrio, nivel, modalidad)
                    .filter(termino -> termino != null && !termino.isBlank())
                    .map(String::trim)
                    .reduce((uno, otro) -> uno + " " + otro)
                    .orElse("");
        }
    }

    public List<DeportePropuesto> deportesODefecto() {
        return deportes == null ? List.of() : deportes;
    }

    public String fraseDeFiltros() {
        return filtros == null ? "" : filtros.comoFrase();
    }

    /** ¿Trae algo aprovechable, o vino vacío? */
    public boolean tieneContenido() {
        return (mensaje != null && !mensaje.isBlank())
                || !deportesODefecto().isEmpty()
                || !fraseDeFiltros().isBlank();
    }
}
