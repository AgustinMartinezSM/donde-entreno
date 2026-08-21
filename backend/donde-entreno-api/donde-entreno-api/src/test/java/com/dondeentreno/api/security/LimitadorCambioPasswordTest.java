package com.dondeentreno.api.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorCambioPasswordTest {

    private static final Instant AHORA = Instant.parse("2026-08-21T12:00:00Z");

    /** Reloj que se puede adelantar a mano, para probar la ventana. */
    private static final class RelojManual extends Clock {
        private Instant instante;

        private RelojManual(Instant inicial) {
            this.instante = inicial;
        }

        private void avanzar(Duration cuanto) {
            instante = instante.plus(cuanto);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @Test
    void sinFallosNoBloquea() {
        LimitadorCambioPassword limitador = new LimitadorCambioPassword(new RelojManual(AHORA));

        assertFalse(limitador.estaBloqueado(1L));
    }

    @Test
    void cuatroFallosNoBloqueanElQuintoSi() {
        LimitadorCambioPassword limitador = new LimitadorCambioPassword(new RelojManual(AHORA));

        for (int i = 0; i < 4; i++) {
            limitador.registrarFallo(1L);
        }
        assertFalse(limitador.estaBloqueado(1L));

        limitador.registrarFallo(1L);
        assertTrue(limitador.estaBloqueado(1L));
    }

    @Test
    void losFallosSonPorUsuario() {
        LimitadorCambioPassword limitador = new LimitadorCambioPassword(new RelojManual(AHORA));

        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo(1L);
        }

        assertTrue(limitador.estaBloqueado(1L));
        assertFalse(limitador.estaBloqueado(2L));
    }

    @Test
    void pasadaLaVentanaElBloqueoSeLevantaSolo() {
        RelojManual reloj = new RelojManual(AHORA);
        LimitadorCambioPassword limitador = new LimitadorCambioPassword(reloj);

        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo(1L);
        }
        assertTrue(limitador.estaBloqueado(1L));

        reloj.avanzar(LimitadorCambioPassword.VENTANA.plusSeconds(1));
        assertFalse(limitador.estaBloqueado(1L));
    }

    @Test
    void elExitoLimpiaElContador() {
        LimitadorCambioPassword limitador = new LimitadorCambioPassword(new RelojManual(AHORA));

        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo(1L);
        }
        assertTrue(limitador.estaBloqueado(1L));

        limitador.registrarExito(1L);
        assertFalse(limitador.estaBloqueado(1L));
    }
}
