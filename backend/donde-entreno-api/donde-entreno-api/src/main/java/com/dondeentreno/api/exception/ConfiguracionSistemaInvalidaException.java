package com.dondeentreno.api.exception;

/**
 * Error controlado para configuracion backend inconsistente.
 */
public class ConfiguracionSistemaInvalidaException extends RuntimeException {

    public ConfiguracionSistemaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
