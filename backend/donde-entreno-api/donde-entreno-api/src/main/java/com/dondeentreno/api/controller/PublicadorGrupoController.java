package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.AvisoGrupoDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.GrupoActividadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * El grupo, del lado del publicador: avisar y moderar lo suyo.
 *
 * No hay endpoint para "leer el grupo" acá: el publicador ve sus
 * avisos y los comentarios a través del mismo camino que un miembro.
 */
@RestController
@RequestMapping("/api/publicador/grupos")
public class PublicadorGrupoController {

    private final GrupoActividadService grupoService;

    public PublicadorGrupoController(GrupoActividadService grupoService) {
        this.grupoService = grupoService;
    }

    @PostMapping("/{actividadId}/avisos")
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoGrupoDTO avisar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @RequestBody Map<String, String> cuerpo
    ) {
        Long imagenId = null;
        String imagen = cuerpo.get("imagenId");

        if (imagen != null && !imagen.isBlank()) {
            try {
                imagenId = Long.valueOf(imagen);
            } catch (NumberFormatException excepcion) {
                /* Foto ilegible: el aviso sale sin ella. */
                imagenId = null;
            }
        }

        return grupoService.avisar(
                extraerUserId(jwt), actividadId, cuerpo.get("texto"), imagenId);
    }

    @DeleteMapping("/avisos/{avisoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarAviso(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long avisoId
    ) {
        grupoService.eliminarAviso(extraerUserId(jwt), avisoId);
    }

    /** El publicador modera su propio grupo (patrón de la Fase 4). */
    @PatchMapping("/comentarios/{comentarioId}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarComentario(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long comentarioId
    ) {
        grupoService.ocultarComentarioPorPublicador(extraerUserId(jwt), comentarioId);
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
            } catch (NumberFormatException excepcion) {
                throw new CredencialesInvalidasException("No autenticado.");
            }
        }

        throw new CredencialesInvalidasException("No autenticado.");
    }
}
