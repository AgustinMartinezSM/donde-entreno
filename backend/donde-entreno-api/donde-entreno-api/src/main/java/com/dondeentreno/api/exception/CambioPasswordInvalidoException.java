package com.dondeentreno.api.exception;

/**
 * El cambio de password no puede hacerse con los datos recibidos
 * (password actual incorrecta, o nueva igual a la actual).
 *
 * Es 400 y no 401 a proposito: el 401 queda reservado a "la sesion no
 * es valida", asi el frontend nunca confunde un error de tipeo con una
 * sesion vencida.
 */
public class CambioPasswordInvalidoException extends RuntimeException {

    public CambioPasswordInvalidoException(String mensaje) {
        super(mensaje);
    }
}
