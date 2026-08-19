package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.FavoritosUsuarioService;
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
 * Favoritos de la cuenta (sync, script 20).
 *
 * Protegido por la regla explicita /api/usuario/** de SecurityConfig:
 * anonimo recibe 401. La accion se acota al usuario del JWT.
 */
@RestController
@RequestMapping("/api/usuario/favoritos")
public class UsuarioFavoritosController {

    private final FavoritosUsuarioService favoritosUsuarioService;

    public UsuarioFavoritosController(FavoritosUsuarioService favoritosUsuarioService) {
        this.favoritosUsuarioService = favoritosUsuarioService;
    }

    @GetMapping
    public List<ActividadDTO> listar(@AuthenticationPrincipal Jwt jwt) {
        return favoritosUsuarioService.listar(extraerUserId(jwt));
    }

    @PutMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        favoritosUsuarioService.guardar(extraerUserId(jwt), slug);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        favoritosUsuarioService.quitar(extraerUserId(jwt), slug);
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
