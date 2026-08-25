package com.dondeentreno.api.controller;

import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.NovedadService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Reacciones a novedades (script 37).
 *
 * PUT/DELETE y no un POST que alterna: el botón es idempotente, así
 * que repetir el request tiene que dar el mismo resultado (mismo
 * patrón que los likes de fotos y el "me interesa" de eventos).
 */
@RestController
@RequestMapping("/api/usuario/novedades")
public class UsuarioMeGustaNovedadController {

    private final NovedadService novedadService;

    public UsuarioMeGustaNovedadController(NovedadService novedadService) {
        this.novedadService = novedadService;
    }

    @PutMapping("/{novedadId}/me-gusta")
    public Map<String, Object> dar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long novedadId
    ) {
        long total = novedadService.darMeGusta(extraerUserId(jwt), novedadId);
        return Map.of("cantidadMeGusta", total, "meGusta", true);
    }

    @DeleteMapping("/{novedadId}/me-gusta")
    public Map<String, Object> quitar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long novedadId
    ) {
        long total = novedadService.quitarMeGusta(extraerUserId(jwt), novedadId);
        return Map.of("cantidadMeGusta", total, "meGusta", false);
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
