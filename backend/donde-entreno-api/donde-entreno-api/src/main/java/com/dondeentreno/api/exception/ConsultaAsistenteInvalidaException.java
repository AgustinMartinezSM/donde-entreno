package com.dondeentreno.api.exception;

/**
 * La consulta al asistente no cumple las reglas de entrada
 * (vacía o más larga que el máximo configurado).
 */
public class ConsultaAsistenteInvalidaException extends RuntimeException {

    public ConsultaAsistenteInvalidaException(String mensaje) {
        super(mensaje);
    }
}
