package com.dondeentreno.api.exception;

/**
 * Error controlado cuando se intenta registrar un email ya usado.
 */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}
