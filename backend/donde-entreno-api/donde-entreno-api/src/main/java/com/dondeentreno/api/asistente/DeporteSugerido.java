package com.dondeentreno.api.asistente;

/**
 * Un deporte que el asistente le propone al usuario, ya cruzado con la
 * realidad de DondeEntreno.
 *
 * La distinción entre "esto es un consejo" y "esto lo tenemos" vive acá y
 * no en el texto: si {@code slug} es null, el deporte no está en el
 * catálogo y la respuesta lo dice; si tiene slug pero {@code publicadas}
 * es cero, existe el deporte pero todavía nadie subió actividades. Son
 * tres estados distintos y el usuario merece saber en cuál está.
 *
 * @param nombre     como se muestra.
 * @param motivo     por qué se lo proponemos, en una línea.
 * @param slug       slug real del catálogo, o null si no está en la base.
 * @param publicadas cuántas actividades publicadas hay hoy. Sale de la
 *                   búsqueda real, nunca de una estimación.
 */
public record DeporteSugerido(
        String nombre,
        String motivo,
        String slug,
        int publicadas
) {

    /** ¿Podemos mandar al usuario a ver algo concreto? */
    public boolean tieneActividades() {
        return slug != null && publicadas > 0;
    }

    /** ¿Es puro consejo general, sin respaldo en el catálogo? */
    public boolean esSoloRecomendacion() {
        return !tieneActividades();
    }
}
