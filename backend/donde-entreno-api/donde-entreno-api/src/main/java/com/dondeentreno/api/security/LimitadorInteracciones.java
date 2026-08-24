package com.dondeentreno.api.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit del tracking de interacciones (Fase 2 social): ventana
 * deslizante de un minuto por IP, en memoria (patrón
 * LimitadorConsultas del asistente — misma justificación: una sola
 * instancia y perder el estado no cuesta nada).
 */
@Component
public class LimitadorInteracciones {

    private static final int MAX_POR_MINUTO = 30;
    private static final int MAX_IPS_RECORDADAS = 10_000;

    private final Clock reloj;
    private final Map<String, Deque<Instant>> eventosPorIp = new ConcurrentHashMap<>();

    public LimitadorInteracciones() {
        this(Clock.systemUTC());
    }

    LimitadorInteracciones(Clock reloj) {
        this.reloj = reloj;
    }

    /** @return true si puede seguir; false si hay que ignorar el evento. */
    public synchronized boolean registrar(String ip) {
        Instant ahora = reloj.instant();
        Instant desdeUnMinuto = ahora.minus(1, ChronoUnit.MINUTES);

        limpiarViejas(desdeUnMinuto);

        Deque<Instant> marcas = eventosPorIp.computeIfAbsent(
                ip,
                clave -> new ArrayDeque<>()
        );

        while (!marcas.isEmpty() && marcas.peekFirst().isBefore(desdeUnMinuto)) {
            marcas.pollFirst();
        }

        if (marcas.size() >= MAX_POR_MINUTO) {
            return false;
        }

        marcas.addLast(ahora);
        return true;
    }

    private void limpiarViejas(Instant desdeUnMinuto) {
        eventosPorIp.entrySet().removeIf(entrada -> {
            Deque<Instant> marcas = entrada.getValue();
            return marcas.isEmpty() || marcas.peekLast().isBefore(desdeUnMinuto);
        });

        if (eventosPorIp.size() > MAX_IPS_RECORDADAS) {
            eventosPorIp.clear();
        }
    }
}
