package com.dondeentreno.api.exception;

/**
 * Excepcion personalizada para filtros de busqueda invalidos
 * recibidos desde la URL.
 *
 * Se lanza cuando un parametro de filtro u ordenamiento llega
 * con un valor que no esta dentro de los permitidos.
 *
 * El GlobalExceptionHandler la convierte en una respuesta 400,
 * para que el cliente sepa que su filtro no se aplico.
 */
public class FiltroInvalidoException extends RuntimeException {

    /**
     * Constructor que recibe el mensaje controlado del error.
     *
     * @param mensaje mensaje descriptivo indicando el parametro invalido
     *                y los valores permitidos.
     */
    public FiltroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
