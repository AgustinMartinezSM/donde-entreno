package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ReporteRequestDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.ReporteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes del usuario autenticado (script 28, Fase 2 social). Bajo
 * /api/usuario/**: anónimo recibe 401 — reportar exige cuenta.
 */
@RestController
@RequestMapping("/api/usuario/reportes")
public class UsuarioReportesController {

    private final ReporteService reporteService;

    public UsuarioReportesController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReporteRequestDTO request
    ) {
        reporteService.reportar(
                extraerUserId(jwt),
                request.getTipoObjeto(),
                request.getObjetoId(),
                request.getMotivo(),
                request.getDetalle()
        );
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
