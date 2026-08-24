package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.CheckinRespuestaDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.CheckinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Check-ins "Entrené acá" del usuario autenticado (script 26). Bajo
 * /api/usuario/**: anónimo recibe 401 y todo se acota al JWT.
 */
@RestController
@RequestMapping("/api/usuario/checkins")
public class UsuarioCheckinsController {

    private final CheckinService checkinService;

    public UsuarioCheckinsController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    /**
     * Registra el check-in de hoy: 201 con fila nueva, 200 si ya había
     * uno hoy (idempotente, sin fila).
     */
    @PostMapping("/{actividadId}")
    public ResponseEntity<CheckinRespuestaDTO> registrar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        CheckinRespuestaDTO respuesta =
                checkinService.registrar(extraerUserId(jwt), actividadId);

        HttpStatus status = respuesta.isRegistradoAhora()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(respuesta);
    }

    /** Deshace el check-in de hoy (idempotente). */
    @org.springframework.web.bind.annotation.DeleteMapping("/{actividadId}")
    public CheckinRespuestaDTO quitarDeHoy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        return checkinService.quitarDeHoy(extraerUserId(jwt), actividadId);
    }

    /** Estado del botón al cargar el detalle logueado. */
    @GetMapping("/{actividadId}/hoy")
    public CheckinRespuestaDTO estadoDeHoy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        return checkinService.estadoDeHoy(extraerUserId(jwt), actividadId);
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
