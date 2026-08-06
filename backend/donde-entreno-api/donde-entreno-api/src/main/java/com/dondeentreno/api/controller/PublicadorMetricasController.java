package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PublicadorMetricasDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.PublicadorMetricasService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas de resumen del publicador autenticado.
 *
 * GET /api/publicador/metricas
 *
 * Protegido por SecurityConfig: /api/publicador/** requiere rol
 * PUBLICADOR (anónimo → 401, otro rol → 403).
 */
@RestController
@RequestMapping("/api/publicador/metricas")
public class PublicadorMetricasController {

    private final PublicadorMetricasService publicadorMetricasService;

    public PublicadorMetricasController(PublicadorMetricasService publicadorMetricasService) {
        this.publicadorMetricasService = publicadorMetricasService;
    }

    @GetMapping
    public PublicadorMetricasDTO obtenerMisMetricas(@AuthenticationPrincipal Jwt jwt) {
        return publicadorMetricasService.obtenerMetricas(extraerUserId(jwt));
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
