package com.dondeentreno.api.exception;

/**
 * Regla de negocio invalida en el flujo de solicitudes de cambio
 * de actividades publicadas. El GlobalExceptionHandler la convierte
 * en una respuesta 400.
 */
public class SolicitudCambioInvalidaException extends RuntimeException {

    public SolicitudCambioInvalidaException(String mensaje) {
        super(mensaje);
    }
}
