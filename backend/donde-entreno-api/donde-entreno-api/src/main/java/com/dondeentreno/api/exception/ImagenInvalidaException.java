package com.dondeentreno.api.exception;

/**
 * Archivo o datos invalidos en la subida de imagenes
 * (tipo no permitido, tamano excedido, contenido que no es imagen).
 * El GlobalExceptionHandler la convierte en una respuesta 400.
 */
public class ImagenInvalidaException extends RuntimeException {

    public ImagenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
