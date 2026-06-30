package com.dondeentreno.api.exception;

/**
 * Excepcion controlada para errores de autenticacion por credenciales invalidas.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
