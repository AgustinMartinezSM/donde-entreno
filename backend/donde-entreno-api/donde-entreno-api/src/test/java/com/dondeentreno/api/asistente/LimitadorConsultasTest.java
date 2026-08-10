package com.dondeentreno.api.asistente;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorConsultasTest {

    /** Reloj que avanza solo cuando el test se lo pide. */
    private static class RelojFijo extends Clock {

        private Instant instante;

        RelojFijo(Instant instante) {
            this.instante = instante;
        }

        void avanzar(long cantidad, ChronoUnit unidad) {
            instante = instante.plus(cantidad, unidad);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    private AsistenteProperties propiedades(int porMinuto, int porHora, int cuotaGemini) {
        AsistenteProperties propiedades = new AsistenteProperties();
        propiedades.setMaxConsultasPorMinuto(porMinuto);
        propiedades.setMaxConsultasPorHora(porHora);
        propiedades.setGeminiDailyLimit(cuotaGemini);
        return propiedades;
    }

    @Test
    void cortaAlSuperarElLimitePorMinutoYSeRecuperaAlPasarLaVentana() {
        RelojFijo reloj = new RelojFijo(Instant.parse("2026-08-08T10:00:00Z"));
        LimitadorConsultas limitador = new LimitadorConsultas(propiedades(2, 100, 30), reloj);

        assertThat(limitador.registrarConsulta("1.1.1.1")).isTrue();
        assertThat(limitador.registrarConsulta("1.1.1.1")).isTrue();
        assertThat(limitador.registrarConsulta("1.1.1.1")).isFalse();

        reloj.avanzar(61, ChronoUnit.SECONDS);

        assertThat(limitador.registrarConsulta("1.1.1.1")).isTrue();
    }

    @Test
    void elLimiteDeUnaIpNoAfectaAOtra() {
        RelojFijo reloj = new RelojFijo(Instant.parse("2026-08-08T10:00:00Z"));
        LimitadorConsultas limitador = new LimitadorConsultas(propiedades(1, 100, 30), reloj);

        assertThat(limitador.registrarConsulta("1.1.1.1")).isTrue();
        assertThat(limitador.registrarConsulta("1.1.1.1")).isFalse();
        assertThat(limitador.registrarConsulta("2.2.2.2")).isTrue();
    }

    @Test
    void cortaAlSuperarElLimitePorHoraAunqueNoSeSupereElDeMinuto() {
        RelojFijo reloj = new RelojFijo(Instant.parse("2026-08-08T10:00:00Z"));
        LimitadorConsultas limitador = new LimitadorConsultas(propiedades(10, 3, 30), reloj);

        for (int intento = 0; intento < 3; intento += 1) {
            assertThat(limitador.registrarConsulta("1.1.1.1")).isTrue();
            reloj.avanzar(5, ChronoUnit.MINUTES);
        }

        assertThat(limitador.registrarConsulta("1.1.1.1")).isFalse();
    }

    @Test
    void laCuotaDiariaDeGeminiSeAgotaYSeRenuevaAlDiaSiguiente() {
        RelojFijo reloj = new RelojFijo(Instant.parse("2026-08-08T10:00:00Z"));
        LimitadorConsultas limitador = new LimitadorConsultas(propiedades(100, 100, 2), reloj);

        assertThat(limitador.consumirCuotaGemini()).isTrue();
        assertThat(limitador.consumirCuotaGemini()).isTrue();
        assertThat(limitador.consumirCuotaGemini()).isFalse();
        assertThat(limitador.llamadasGeminiDelDia()).isEqualTo(2);

        reloj.avanzar(1, ChronoUnit.DAYS);

        assertThat(limitador.consumirCuotaGemini()).isTrue();
        assertThat(limitador.llamadasGeminiDelDia()).isEqualTo(1);
    }
}
