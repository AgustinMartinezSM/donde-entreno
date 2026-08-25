package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.NovedadDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.NovedadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Canal de novedades del publicador autenticado (Fase 8). Bajo
 * /api/publicador/** → exige rol PUBLICADOR.
 */
@RestController
@RequestMapping("/api/publicador/novedades")
public class PublicadorNovedadController {

    private final NovedadService novedadService;

    public PublicadorNovedadController(NovedadService novedadService) {
        this.novedadService = novedadService;
    }

    @GetMapping
    public List<NovedadDTO> listarMias(@AuthenticationPrincipal Jwt jwt) {
        return novedadService.listarMias(extraerUserId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NovedadDTO publicar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> cuerpo
    ) {
        Long imagenId = null;
        String imagen = cuerpo.get("imagenId");

        if (imagen != null && !imagen.isBlank()) {
            try {
                imagenId = Long.valueOf(imagen);
            } catch (NumberFormatException excepcion) {
                /* Foto ilegible: se publica sin ella, el texto es lo que importa. */
                imagenId = null;
            }
        }

        return novedadService.publicar(
                extraerUserId(jwt),
                cuerpo.get("texto"),
                imagenId
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPropia(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        novedadService.eliminarPropia(extraerUserId(jwt), id);
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
