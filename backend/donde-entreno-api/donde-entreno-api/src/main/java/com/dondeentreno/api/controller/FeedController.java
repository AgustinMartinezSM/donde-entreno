package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.SeguimientoPublicadorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feed de novedades del usuario (capa social, Bloque 8).
 *
 * GET /api/usuario/feed/actividades
 *
 * Últimas actividades publicadas de los publicadores que sigue el
 * usuario autenticado. Protegido por SecurityConfig: /api/usuario/**
 * cae en anyRequest().authenticated() → anónimo recibe 401.
 */
@RestController
@RequestMapping("/api/usuario/feed")
public class FeedController {

    private final SeguimientoPublicadorService seguimientoPublicadorService;

    public FeedController(SeguimientoPublicadorService seguimientoPublicadorService) {
        this.seguimientoPublicadorService = seguimientoPublicadorService;
    }

    @GetMapping("/actividades")
    public List<ActividadDTO> obtenerFeedActividades(@AuthenticationPrincipal Jwt jwt) {
        return seguimientoPublicadorService.obtenerFeedActividades(extraerUserId(jwt));
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
            } catch (NumberFormatException exception) {
                throw new CredencialesInvalidasException("No autenticado.");
            }
        }

        throw new CredencialesInvalidasException("No autenticado.");
    }
}
