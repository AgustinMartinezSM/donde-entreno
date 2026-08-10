package com.dondeentreno.api.asistente;

import java.util.List;

/**
 * Lo que el asistente sabe DE UN DEPORTE, con independencia de si hoy hay
 * actividades publicadas de ese deporte en DondeEntreno.
 *
 * Esta es la distinción que hace posible el asistente V2: hasta ahora, un
 * deporte que no estaba en la base simplemente no existía para el
 * asistente, y por eso no podía recomendar pádel, escalada o baile.
 *
 * Ojo con lo que este record NO es: no es un catálogo espejo y no decide
 * qué existe. Quién existe lo sigue diciendo FiltroService, que sale de la
 * base. Acá solo vive la descripción y el "carácter" de cada deporte, que
 * es conocimiento general y no cambia porque un club publique o deje de
 * publicar.
 *
 * @param nombre       cómo se muestra ("Pádel", "Cross Training").
 * @param explicacion  una línea, la que se le muestra al usuario.
 * @param puntajeBase  cuánto pesa cuando la consulta no trae ninguna señal
 *                     ("¿algún deporte que recomiendes?"). Ordena la
 *                     recomendación por defecto y desempata siempre igual.
 * @param intensidad   qué tan exigente es.
 * @param social       cuánto se comparte con otra gente.
 * @param impacto      cuánto castiga articulaciones; manda para
 *                     "me canso rápido" y para cualquier tema de salud.
 * @param combate      si implica pelear o pegarle a alguien. Es su propio
 *                     campo y no un valor de "contacto" porque el rechazo
 *                     más común es exactamente ese: "no me gustan los
 *                     deportes de pelea". El fútbol tiene contacto físico y
 *                     no entra acá.
 * @param variedad     si los ejercicios cambian de una clase a la otra.
 * @param competitivo  si hay marcador, torneo o rival.
 * @param aire         si mejora resistencia cardiovascular.
 * @param alias        cómo lo puede nombrar la gente ("bici", "spinning").
 */
public record DeporteConocido(
        String nombre,
        String explicacion,
        int puntajeBase,
        Escala intensidad,
        Escala social,
        Escala impacto,
        boolean combate,
        boolean variedad,
        boolean competitivo,
        boolean aire,
        List<String> alias
) {

    /** Escala de tres valores; alcanza para puntuar y se lee bien. */
    public enum Escala {
        BAJA, MEDIA, ALTA
    }
}
