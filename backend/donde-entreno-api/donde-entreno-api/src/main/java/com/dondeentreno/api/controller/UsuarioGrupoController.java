package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.AvisoGrupoDTO;
import com.dondeentreno.api.dto.ComentarioAvisoDTO;
import com.dondeentreno.api.dto.GrupoActividadDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.GrupoActividadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * El grupo, del lado de quien participa. Bajo /api/usuario/** → exige
 * sesión.
 *
 * NO hay endpoint que devuelva el contenido de un grupo a alguien que
 * no sea miembro: la respuesta trae `esMiembro=false` y la lista de
 * avisos VACÍA, resuelto en el service.
 */
@RestController
@RequestMapping("/api/usuario/grupos")
public class UsuarioGrupoController {

    private final GrupoActividadService grupoService;

    public UsuarioGrupoController(GrupoActividadService grupoService) {
        this.grupoService = grupoService;
    }

    @GetMapping("/{actividadId}")
    public GrupoActividadDTO verGrupo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        return grupoService.verGrupo(extraerUserId(jwt), actividadId);
    }

    @PutMapping("/{actividadId}/miembros")
    public GrupoActividadDTO unirse(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        return grupoService.unirse(extraerUserId(jwt), actividadId);
    }

    @DeleteMapping("/{actividadId}/miembros")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        grupoService.salir(extraerUserId(jwt), actividadId);
    }

    @GetMapping("/avisos/{avisoId}")
    public AvisoGrupoDTO verAviso(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long avisoId
    ) {
        return grupoService.verAviso(extraerUserId(jwt), avisoId);
    }

    @PostMapping("/avisos/{avisoId}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioAvisoDTO comentar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long avisoId,
            @RequestBody Map<String, String> cuerpo
    ) {
        return grupoService.comentar(extraerUserId(jwt), avisoId, cuerpo.get("texto"));
    }

    @DeleteMapping("/comentarios/{comentarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarComentario(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long comentarioId
    ) {
        grupoService.eliminarComentarioPropio(extraerUserId(jwt), comentarioId);
    }

    @PutMapping("/avisos/{avisoId}/me-gusta")
    public Map<String, Object> darMeGusta(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long avisoId
    ) {
        long total = grupoService.darMeGusta(extraerUserId(jwt), avisoId);
        return Map.of("cantidadMeGusta", total, "meGusta", true);
    }

    @DeleteMapping("/avisos/{avisoId}/me-gusta")
    public Map<String, Object> quitarMeGusta(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long avisoId
    ) {
        long total = grupoService.quitarMeGusta(extraerUserId(jwt), avisoId);
        return Map.of("cantidadMeGusta", total, "meGusta", false);
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
