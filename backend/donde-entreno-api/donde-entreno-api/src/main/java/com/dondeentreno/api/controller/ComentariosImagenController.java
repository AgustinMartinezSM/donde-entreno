package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ComentarioImagenDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.ComentarioImagenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Comentarios en fotos (script 30): GET público; comentar/borrar bajo
 * /api/usuario/**; ocultar del dueño bajo /api/publicador/** (el
 * service valida que la foto sea suya).
 */
@RestController
public class ComentariosImagenController {

    private final ComentarioImagenService comentarioImagenService;

    public ComentariosImagenController(ComentarioImagenService comentarioImagenService) {
        this.comentarioImagenService = comentarioImagenService;
    }

    @GetMapping("/api/imagenes/{imagenId}/comentarios")
    public List<ComentarioImagenDTO> listar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long imagenId
    ) {
        return comentarioImagenService.listarDe(imagenId, extraerUserIdOpcional(jwt));
    }

    @PostMapping("/api/usuario/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioImagenDTO comentar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> cuerpo
    ) {
        Long imagenId;
        try {
            imagenId = Long.valueOf(cuerpo.get("imagenId"));
        } catch (NumberFormatException | NullPointerException excepcion) {
            throw new com.dondeentreno.api.exception.FiltroInvalidoException(
                    "Falta la foto del comentario."
            );
        }

        return comentarioImagenService.comentar(
                extraerUserId(jwt),
                imagenId,
                cuerpo.get("texto")
        );
    }

    @DeleteMapping("/api/usuario/comentarios/{comentarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPropio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long comentarioId
    ) {
        comentarioImagenService.eliminarPropio(extraerUserId(jwt), comentarioId);
    }

    @PatchMapping("/api/publicador/comentarios/{comentarioId}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarEnMiFoto(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long comentarioId
    ) {
        comentarioImagenService.ocultarPorPublicador(extraerUserId(jwt), comentarioId);
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
