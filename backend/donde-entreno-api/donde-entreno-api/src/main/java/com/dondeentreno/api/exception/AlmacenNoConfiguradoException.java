package com.dondeentreno.api.exception;

/**
 * El almacenamiento de archivos (Supabase Storage) no está configurado
 * en este entorno: faltan la URL del proyecto o la service key.
 *
 * La aplicación arranca y funciona igual sin esa configuración; solo
 * las operaciones que necesitan el storage (subir, aprobar, firmar,
 * eliminar) responden 503 con un mensaje claro.
 */
public class AlmacenNoConfiguradoException extends RuntimeException {

    public AlmacenNoConfiguradoException() {
        super("La carga de imágenes no está disponible en este momento.");
    }
}
