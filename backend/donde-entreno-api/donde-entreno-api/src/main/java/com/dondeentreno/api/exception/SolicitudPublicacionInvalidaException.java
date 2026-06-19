package com.dondeentreno.api.exception;

/**
 * Excepcion personalizada para reglas de negocio invalidas
 * al crear solicitudes publicas de publicacion.
 */
public class SolicitudPublicacionInvalidaException extends RuntimeException {

    /**
     * Constructor que recibe el mensaje controlado del error.
     *
     * @param mensaje mensaje descriptivo de la regla incumplida.
     */
    public SolicitudPublicacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
