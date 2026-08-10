package com.dondeentreno.api.asistente;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límites de uso del asistente.
 *
 * Dos controles distintos, con propósitos distintos:
 *
 * 1. Por IP: evita que una sola persona (o un script) monopolice el
 *    endpoint público. Ventana deslizante en memoria.
 * 2. Global diario de Gemini: es el tope de gasto. Al agotarse NO se
 *    devuelve error: el asistente sigue contestando con el motor local,
 *    que es gratis. El usuario no se entera de que se acabó la cuota.
 *
 * En memoria a propósito: Render corre una sola instancia y el estado se
 * puede perder sin consecuencias (en el peor caso se reinicia el contador
 * tras un deploy). Una tabla en Supabase para esto sería sobreingeniería
 * y agregaría escrituras a la base en cada consulta.
 */
public class LimitadorConsultas {

    /**
     * Tope de IPs recordadas. Sin esto, el mapa crece sin límite con el
     * tráfico y termina siendo una fuga de memoria: cada IP nueva deja su
     * entrada para siempre.
     */
    private static final int MAX_IPS_RECORDADAS = 10_000;

    private final AsistenteProperties propiedades;
    private final Clock reloj;
    private final Map<String, Deque<Instant>> consultasPorIp = new ConcurrentHashMap<>();

    private LocalDate diaDelConteo;
    private int llamadasGeminiDelDia;

    public LimitadorConsultas(AsistenteProperties propiedades, Clock reloj) {
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.diaDelConteo = LocalDate.now(reloj);
    }

    /**
     * Registra una consulta de esa IP y dice si está dentro del límite.
     *
     * @param ip identificador del cliente (ver AsistenteController).
     * @return true si puede seguir; false si hay que responder 429.
     */
    public synchronized boolean registrarConsulta(String ip) {
        Instant ahora = reloj.instant();
        Instant desdeUnaHora = ahora.minus(1, ChronoUnit.HOURS);
        Instant desdeUnMinuto = ahora.minus(1, ChronoUnit.MINUTES);

        limpiarIpsViejas(desdeUnaHora);

        Deque<Instant> marcas = consultasPorIp.computeIfAbsent(
                ip,
                clave -> new ArrayDeque<>()
        );

        /* Fuera de la ventana de una hora ya no cuentan para nada. */
        while (!marcas.isEmpty() && marcas.peekFirst().isBefore(desdeUnaHora)) {
            marcas.pollFirst();
        }

        long enElUltimoMinuto = marcas.stream()
                .filter(marca -> !marca.isBefore(desdeUnMinuto))
                .count();

        if (enElUltimoMinuto >= propiedades.getMaxConsultasPorMinuto()
                || marcas.size() >= propiedades.getMaxConsultasPorHora()) {
            return false;
        }

        marcas.addLast(ahora);
        return true;
    }

    /**
     * Consume una unidad del tope diario de Gemini.
     *
     * @return true si quedaba cuota (y queda consumida); false si se agotó.
     */
    public synchronized boolean consumirCuotaGemini() {
        LocalDate hoy = LocalDate.now(reloj);

        if (!hoy.equals(diaDelConteo)) {
            diaDelConteo = hoy;
            llamadasGeminiDelDia = 0;
        }

        if (llamadasGeminiDelDia >= propiedades.getGeminiDailyLimit()) {
            return false;
        }

        llamadasGeminiDelDia += 1;
        return true;
    }

    /** Para el log de metadata mínima y para diagnóstico. */
    public synchronized int llamadasGeminiDelDia() {
        return LocalDate.now(reloj).equals(diaDelConteo) ? llamadasGeminiDelDia : 0;
    }

    /**
     * Saca del mapa las IPs sin actividad en la última hora, y si aun así
     * sigue creciendo por encima del tope, lo vacía entero. Perder el
     * historial solo significa que alguien recupera su cuota antes de
     * tiempo; quedarse sin memoria en Render es bastante peor.
     */
    private void limpiarIpsViejas(Instant desdeUnaHora) {
        consultasPorIp.entrySet().removeIf(entrada -> {
            Deque<Instant> marcas = entrada.getValue();
            return marcas.isEmpty() || marcas.peekLast().isBefore(desdeUnaHora);
        });

        if (consultasPorIp.size() > MAX_IPS_RECORDADAS) {
            consultasPorIp.clear();
        }
    }
}
