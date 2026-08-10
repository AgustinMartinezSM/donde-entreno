package com.dondeentreno.api.asistente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.stream.Stream;

/**
 * Lo que devuelve el motor remoto: términos sueltos, nada más.
 *
 * A propósito NO incluye el texto de la respuesta. El modelo no le habla
 * al usuario: solo dice "esto suena a yoga, en Constitución, nivel
 * principiante", y el backend vuelve a resolver esos términos contra el
 * catálogo real y escribe la respuesta él mismo.
 *
 * Consecuencia buscada: por más que alguien intente inyectar
 * instrucciones en el mensaje, el modelo no tiene forma de poner una
 * sola palabra en la pantalla del usuario ni de inventar un enlace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InterpretacionRemota(
        String deporte,
        String categoria,
        String barrio,
        String nivel,
        String modalidad
) {

    /**
     * Junta los términos en una frase para volver a pasarla por el mismo
     * resolutor determinístico que usa el camino local. Si el modelo
     * inventó algo que no está en el catálogo, no matchea y se descarta
     * solo: no hace falta una lista negra.
     */
    public String comoFrase() {
        return Stream.of(deporte, categoria, barrio, nivel, modalidad)
                .filter(termino -> termino != null && !termino.isBlank())
                .map(String::trim)
                .reduce((uno, otro) -> uno + " " + otro)
                .orElse("");
    }
}
