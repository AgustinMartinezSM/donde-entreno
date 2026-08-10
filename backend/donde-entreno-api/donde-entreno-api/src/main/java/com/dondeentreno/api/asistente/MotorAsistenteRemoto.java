package com.dondeentreno.api.asistente;

import java.util.Optional;

/**
 * Motor conversacional externo del asistente.
 *
 * Es una interfaz y no la clase concreta por dos motivos: los tests corren
 * sin red ni credenciales, y cambiar de proveedor no debería tocar el
 * servicio.
 *
 * Contrato: NUNCA lanza. Cualquier problema (apagado, sin cuota, timeout,
 * 400, JSON ilegible) devuelve Optional vacío, y el asistente responde con
 * el recomendador determinístico. El usuario no se entera.
 */
public interface MotorAsistenteRemoto {

    /** ¿Está encendido y con credenciales? */
    boolean estaDisponible();

    /**
     * Pide una respuesta conversacional.
     *
     * Lo que devuelve es una propuesta sin validar: el servicio la
     * sanitiza, filtra los deportes contra el catálogo y los rechazos, y
     * escribe él mismo cualquier afirmación sobre qué hay publicado.
     */
    Optional<RespuestaModelo> conversar(ConsultaRemota consulta);
}
