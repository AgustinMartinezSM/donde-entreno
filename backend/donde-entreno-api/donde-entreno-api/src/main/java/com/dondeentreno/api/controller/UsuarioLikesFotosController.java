package com.dondeentreno.api.controller;

import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.LikesFotosService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Likes en fotos del usuario autenticado (script 23, bloque 14). Bajo
 * /api/usuario/**: anonimo recibe 401 y todo se acota al JWT.
 */
@RestController
@RequestMapping("/api/usuario/likes-fotos")
public class UsuarioLikesFotosController {

    private final LikesFotosService likesFotosService;

    public UsuarioLikesFotosController(LikesFotosService likesFotosService) {
        this.likesFotosService = likesFotosService;
    }

    /** Ids de las fotos con like propio, para pintar corazones. */
    @GetMapping
    public List<Long> listar(@AuthenticationPrincipal Jwt jwt) {
        return likesFotosService.listarIds(extraerUserId(jwt));
    }

    @PutMapping("/{imagenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long imagenId) {
        likesFotosService.dar(extraerUserId(jwt), imagenId);
    }

    @DeleteMapping("/{imagenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long imagenId) {
        likesFotosService.quitar(extraerUserId(jwt), imagenId);
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
