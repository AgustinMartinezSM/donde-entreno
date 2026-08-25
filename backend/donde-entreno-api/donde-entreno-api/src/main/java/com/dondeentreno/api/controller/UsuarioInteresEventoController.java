package com.dondeentreno.api.controller;

import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.EventoDeportivoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * "Me interesa" sobre un evento (Fase 9). PUT/DELETE y no POST
 * toggle: el botón es idempotente, así que repetir el request tiene
 * que dar el mismo resultado (mismo patrón que fotos guardadas).
 *
 * Devuelve el contador actualizado para que el frontend no tenga que
 * pedir el evento de nuevo.
 */
@RestController
@RequestMapping("/api/usuario/eventos")
public class UsuarioInteresEventoController {

    private final EventoDeportivoService eventoDeportivoService;

    public UsuarioInteresEventoController(EventoDeportivoService eventoDeportivoService) {
        this.eventoDeportivoService = eventoDeportivoService;
    }

    @PutMapping("/{eventoId}/interes")
    public Map<String, Object> marcar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventoId
    ) {
        long total = eventoDeportivoService.marcarInteres(extraerUserId(jwt), eventoId);
        return Map.of("cantidadInteresados", total, "meInteresa", true);
    }

    @DeleteMapping("/{eventoId}/interes")
    public Map<String, Object> quitar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventoId
    ) {
        long total = eventoDeportivoService.quitarInteres(extraerUserId(jwt), eventoId);
        return Map.of("cantidadInteresados", total, "meInteresa", false);
    }

    private Long extraerUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Object userId = jwt.getClaim("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String texto) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException excepcion) {
                throw new CredencialesInvalidasException("No autenticado.");
            }
        }

        throw new CredencialesInvalidasException("No autenticado.");
    }
}
