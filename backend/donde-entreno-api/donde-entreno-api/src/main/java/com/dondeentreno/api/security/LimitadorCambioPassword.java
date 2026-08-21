package com.dondeentreno.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno de fuerza bruta sobre el cambio de password (fase 5a).
 *
 * El escenario que frena es concreto: alguien con una sesion robada
 * (access token ajeno) probando passwords actuales para tomar la cuenta
 * completa. Cinco intentos fallidos en quince minutos y el endpoint
 * responde 429 para ese usuario.
 *
 * En memoria a proposito, igual que LimitadorConsultas del asistente:
 * Render corre una sola instancia y perder el estado en un deploy solo
 * significa que el contador arranca de cero.
 */
@Component
public class LimitadorCambioPassword {

    static final int MAX_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);

    /** Tope de usuarios recordados: sin esto el mapa seria una fuga de memoria. */
    private static final int MAX_USUARIOS_RECORDADOS = 10_000;

    private final Clock reloj;
    private final Map<Long, Deque<Instant>> fallosPorUsuario = new ConcurrentHashMap<>();

    @Autowired
    public LimitadorCambioPassword() {
        this(Clock.systemUTC());
    }

    LimitadorCambioPassword(Clock reloj) {
        this.reloj = reloj;
    }

    /** true si el usuario agoto sus intentos y hay que responder 429. */
    public synchronized boolean estaBloqueado(Long usuarioId) {
        Deque<Instant> fallos = fallosPorUsuario.get(usuarioId);

        if (fallos == null) {
            return false;
        }

        Instant desdeVentana = reloj.instant().minus(VENTANA);

        while (!fallos.isEmpty() && fallos.peekFirst().isBefore(desdeVentana)) {
            fallos.pollFirst();
        }

        if (fallos.isEmpty()) {
            fallosPorUsuario.remove(usuarioId);
            return false;
        }

        return fallos.size() >= MAX_FALLOS;
    }

    /**
     * Registra una password actual incorrecta. Se llama ANTES de tirar la
     * excepcion del 400: el limitador vive en memoria, asi que el rollback
     * de la transaccion no lo deshace — un fallo cuenta siempre.
     */
    public synchronized void registrarFallo(Long usuarioId) {
        if (fallosPorUsuario.size() > MAX_USUARIOS_RECORDADOS) {
            /* Perder historial = alguien recupera intentos antes; sin memoria es peor. */
            fallosPorUsuario.clear();
        }

        fallosPorUsuario
                .computeIfAbsent(usuarioId, clave -> new ArrayDeque<>())
                .addLast(reloj.instant());
    }

    /** Un cambio exitoso limpia el contador: la persona probo que sabe la password. */
    public synchronized void registrarExito(Long usuarioId) {
        fallosPorUsuario.remove(usuarioId);
    }
}
