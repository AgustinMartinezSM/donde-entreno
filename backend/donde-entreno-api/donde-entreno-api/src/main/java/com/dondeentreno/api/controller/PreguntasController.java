package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PreguntaActividadDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.PreguntaActividadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Preguntas y respuestas (script 29): GET público; preguntar/borrar
 * bajo /api/usuario/**; responder bajo /api/publicador/** (solo el
 * dueño de la actividad, validado en el service).
 */
@RestController
public class PreguntasController {

    private final PreguntaActividadService preguntaActividadService;

    public PreguntasController(PreguntaActividadService preguntaActividadService) {
        this.preguntaActividadService = preguntaActividadService;
    }

    @GetMapping("/api/actividades/{actividadId}/preguntas")
    public List<PreguntaActividadDTO> listar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        return preguntaActividadService.listarDe(actividadId, extraerUserIdOpcional(jwt));
    }

    @PostMapping("/api/usuario/preguntas")
    @ResponseStatus(HttpStatus.CREATED)
    public PreguntaActividadDTO preguntar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> cuerpo
    ) {
        Long actividadId;
        try {
            actividadId = Long.valueOf(cuerpo.get("actividadId"));
        } catch (NumberFormatException | NullPointerException excepcion) {
            throw new com.dondeentreno.api.exception.FiltroInvalidoException(
                    "Falta la actividad de la pregunta."
            );
        }

        return preguntaActividadService.preguntar(
                extraerUserId(jwt),
                actividadId,
                cuerpo.get("pregunta")
        );
    }

    @DeleteMapping("/api/usuario/preguntas/{preguntaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPropia(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long preguntaId
    ) {
        preguntaActividadService.eliminarPropia(extraerUserId(jwt), preguntaId);
    }

    @PostMapping("/api/publicador/preguntas/{preguntaId}/respuesta")
    public PreguntaActividadDTO responder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long preguntaId,
            @RequestBody Map<String, String> cuerpo
    ) {
        return preguntaActividadService.responder(
                extraerUserId(jwt),
                preguntaId,
                cuerpo.get("respuesta")
        );
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
