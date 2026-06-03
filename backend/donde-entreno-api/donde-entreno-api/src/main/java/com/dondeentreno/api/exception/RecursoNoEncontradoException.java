package com.dondeentreno.api.exception;

/**
 * Excepción personalizada para recursos no encontrados.
 *
 * La vamos a usar cuando alguien pide un recurso que no existe
 * o que no está disponible públicamente.
 *
 * Ejemplos:
 * - Actividad no encontrada
 * - Perfil publicador no encontrado
 * - Deporte no encontrado
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Constructor que recibe el mensaje del error.
     *
     * @param mensaje mensaje descriptivo del error.
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
