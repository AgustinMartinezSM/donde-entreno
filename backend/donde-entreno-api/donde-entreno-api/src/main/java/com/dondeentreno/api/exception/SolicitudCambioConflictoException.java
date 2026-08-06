package com.dondeentreno.api.exception;

/**
 * Conflicto en el flujo de solicitudes de cambio: la actividad ya
 * tiene una solicitud abierta (PENDIENTE o EN_REVISION). El
 * GlobalExceptionHandler la convierte en una respuesta 409.
 */
public class SolicitudCambioConflictoException extends RuntimeException {

    public SolicitudCambioConflictoException(String mensaje) {
        super(mensaje);
    }
}
