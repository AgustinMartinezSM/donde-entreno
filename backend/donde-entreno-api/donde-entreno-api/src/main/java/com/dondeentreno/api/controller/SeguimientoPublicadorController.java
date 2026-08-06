package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.EstadoSeguimientoDTO;
import com.dondeentreno.api.dto.SeguimientoPublicadorDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.SeguimientoPublicadorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Seguir / dejar de seguir publicadores (capa social, Bloque 8).
 *
 * Protegido por SecurityConfig: /api/usuario/** cae en
 * anyRequest().authenticated() → anónimo recibe 401. La acción se acota
 * al usuario del JWT, así que cualquier cuenta logueada puede seguir.
 */
@RestController
@RequestMapping("/api/usuario/seguimientos/publicadores")
public class SeguimientoPublicadorController {

    private final SeguimientoPublicadorService seguimientoPublicadorService;

    public SeguimientoPublicadorController(SeguimientoPublicadorService seguimientoPublicadorService) {
        this.seguimientoPublicadorService = seguimientoPublicadorService;
    }

    @PostMapping("/{perfilPublicadorId}")
    public EstadoSeguimientoDTO seguir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long perfilPublicadorId
    ) {
        return seguimientoPublicadorService.seguir(extraerUserId(jwt), perfilPublicadorId);
    }

    @DeleteMapping("/{perfilPublicadorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dejarDeSeguir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long perfilPublicadorId
    ) {
        seguimientoPublicadorService.dejarDeSeguir(extraerUserId(jwt), perfilPublicadorId);
    }

    @GetMapping
    public List<SeguimientoPublicadorDTO> listarSeguidos(@AuthenticationPrincipal Jwt jwt) {
        return seguimientoPublicadorService.listarSeguidos(extraerUserId(jwt));
    }

    @GetMapping("/{perfilPublicadorId}/estado")
    public EstadoSeguimientoDTO estado(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long perfilPublicadorId
    ) {
        return seguimientoPublicadorService.estado(extraerUserId(jwt), perfilPublicadorId);
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
