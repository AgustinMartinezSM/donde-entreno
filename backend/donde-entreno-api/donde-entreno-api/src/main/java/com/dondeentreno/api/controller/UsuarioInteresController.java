package com.dondeentreno.api.controller;

import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.InteresActividadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * El flujo "quiero probar / ya probé" del usuario autenticado
 * (script 29). Bajo /api/usuario/**: anónimo recibe 401.
 */
@RestController
@RequestMapping("/api/usuario/intereses")
public class UsuarioInteresController {

    private final InteresActividadService interesActividadService;

    public UsuarioInteresController(InteresActividadService interesActividadService) {
        this.interesActividadService = interesActividadService;
    }

    @PutMapping("/{actividadId}")
    public Map<String, String> marcar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @RequestBody Map<String, String> cuerpo
    ) {
        String estado = interesActividadService.marcar(
                extraerUserId(jwt),
                actividadId,
                cuerpo.get("estado")
        );

        return Map.of("estado", estado);
    }

    @DeleteMapping("/{actividadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long actividadId) {
        interesActividadService.quitar(extraerUserId(jwt), actividadId);
    }

    /** Estado propio: {"estado": "QUIERO_PROBAR" | "YA_PROBE" | null}. */
    @GetMapping("/{actividadId}")
    public Map<String, String> estado(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("estado", interesActividadService.estadoDe(extraerUserId(jwt), actividadId));
        return respuesta;
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
