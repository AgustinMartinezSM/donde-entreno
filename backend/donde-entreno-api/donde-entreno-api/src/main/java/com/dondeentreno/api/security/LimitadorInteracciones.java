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
    private static final int MAX_LARGO_IP = 60;

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

    /**
     * Identificador del cliente para el rate limit: detrás del proxy de
     * Render la IP real viaja en X-Forwarded-For.
     *
     * Vive acá y no en un controller porque desde la Fase 5 son DOS los
     * que registran interacciones (actividad y perfil) y la resolución
     * tiene que ser idéntica en los dos.
     */
    public static String identificadorDe(jakarta.servlet.http.HttpServletRequest peticion) {
        String reenviadas = peticion.getHeader("X-Forwarded-For");

        if (reenviadas != null && !reenviadas.isBlank()) {
            String primera = reenviadas.split(",")[0].trim();

            if (!primera.isEmpty()) {
                return recortar(primera);
            }
        }

        String remota = peticion.getRemoteAddr();

        return remota == null ? "desconocida" : recortar(remota);
    }

    private static String recortar(String valor) {
        return valor.length() <= MAX_LARGO_IP ? valor : valor.substring(0, MAX_LARGO_IP);
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
