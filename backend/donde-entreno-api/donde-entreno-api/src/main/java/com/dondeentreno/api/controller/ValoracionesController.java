package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ResumenValoracionesDTO;
import com.dondeentreno.api.dto.ValoracionPublicaDTO;
import com.dondeentreno.api.dto.ValorarRequestDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.ValoracionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Valoraciones (script 29): el GET es público (el visitante ve la
 * confianza); crear/editar/eliminar exigen sesión y viven bajo
 * /api/usuario/**. Con sesión presente, el GET marca cuál es tuya.
 */
@RestController
public class ValoracionesController {

    private final ValoracionService valoracionService;

    public ValoracionesController(ValoracionService valoracionService) {
        this.valoracionService = valoracionService;
    }

    /** Público: resumen + lista paginada de visibles. */
    @GetMapping("/api/actividades/{actividadId}/valoraciones")
    public ResumenValoracionesDTO resumen(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return valoracionService.resumenDe(
                actividadId,
                extraerUserIdOpcional(jwt),
                page,
                size
        );
    }

    @PutMapping("/api/usuario/valoraciones/{actividadId}")
    public ValoracionPublicaDTO valorar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @Valid @RequestBody ValorarRequestDTO request
    ) {
        return valoracionService.valorar(
                extraerUserId(jwt),
                actividadId,
                request.getPuntaje(),
                request.getComentario(),
                request.getTags()
        );
    }

    @DeleteMapping("/api/usuario/valoraciones/{actividadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPropia(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        valoracionService.eliminarPropia(extraerUserId(jwt), actividadId);
    }

    private Long extraerUserIdOpcional(Jwt jwt) {
        try {
            return extraerUserId(jwt);
        } catch (CredencialesInvalidasException excepcion) {
            return null;
        }
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
