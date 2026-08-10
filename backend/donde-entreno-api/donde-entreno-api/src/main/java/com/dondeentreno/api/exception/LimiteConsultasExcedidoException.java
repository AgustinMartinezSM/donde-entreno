package com.dondeentreno.api.exception;

/**
 * Se superó el límite de consultas por IP del asistente.
 *
 * Distinto del tope diario de Gemini: ese no da error, cae al motor
 * local, que es gratis y no tiene por qué racionarse.
 */
public class LimiteConsultasExcedidoException extends RuntimeException {

    public LimiteConsultasExcedidoException(String mensaje) {
        super(mensaje);
    }
}
