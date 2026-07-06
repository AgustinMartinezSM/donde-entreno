package com.dondeentreno.api.exception;

/**
 * Error controlado para reglas de negocio de registro.
 */
public class RegistroInvalidoException extends RuntimeException {

    public RegistroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
