package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.NotificacionDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Notificaciones internas del usuario autenticado (script 28, Fase 2
 * social). Bajo /api/usuario/**: anónimo recibe 401.
 */
@RestController
@RequestMapping("/api/usuario/notificaciones")
public class UsuarioNotificacionesController {

    private final NotificacionService notificacionService;

    public UsuarioNotificacionesController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public PaginaResponseDTO<NotificacionDTO> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificacionService.listar(extraerUserId(jwt), page, size);
    }

    /** El número de la campanita (lo pide el polling). */
    @GetMapping("/contador")
    public Map<String, Long> contarNoLeidas(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("noLeidas", notificacionService.contarNoLeidas(extraerUserId(jwt)));
    }

    @PatchMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        notificacionService.marcarLeida(extraerUserId(jwt), id);
    }

    @PatchMapping("/todas-leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLeidas(@AuthenticationPrincipal Jwt jwt) {
        notificacionService.marcarTodasLeidas(extraerUserId(jwt));
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
