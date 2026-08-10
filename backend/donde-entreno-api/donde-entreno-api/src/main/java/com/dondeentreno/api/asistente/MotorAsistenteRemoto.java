package com.dondeentreno.api.asistente;

import java.util.Optional;

/**
 * Motor remoto del asistente (hoy Gemini).
 *
 * Su único trabajo es TRADUCIR: recibe el mensaje que el motor local no
 * supo interpretar y devuelve términos del catálogo. No escribe la
 * respuesta que ve el usuario ni arma enlaces; de eso se sigue ocupando
 * AsistenteService con datos de la base.
 *
 * Cualquier problema (no configurado, error HTTP, timeout, respuesta
 * ilegible) se resuelve devolviendo Optional.empty(): el asistente cae al
 * motor local y el usuario nunca ve un error.
 */
public interface MotorAsistenteRemoto {

    /** ¿Está encendido y con credenciales? */
    boolean estaDisponible();

    /**
     * Traduce el mensaje a términos del catálogo.
     *
     * @param texto mensaje del usuario.
     * @param terminosValidos catálogo real que el modelo puede usar.
     * @return términos sueltos para volver a resolver localmente, o vacío.
     */
    Optional<InterpretacionRemota> interpretar(String texto, String terminosValidos);
}
