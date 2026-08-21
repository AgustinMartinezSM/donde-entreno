package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.AvatarUsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Avatar del usuario autenticado (fase 5d). Bajo /api/usuario/**, que
 * exige sesion por regla explicita de SecurityConfig.
 */
@RestController
@RequestMapping("/api/usuario/avatar")
public class UsuarioAvatarController {

    private final AvatarUsuarioService avatarUsuarioService;

    public UsuarioAvatarController(AvatarUsuarioService avatarUsuarioService) {
        this.avatarUsuarioService = avatarUsuarioService;
    }

    /** Sube o reemplaza el avatar. Devuelve el usuario actualizado. */
    @PutMapping
    public UsuarioActualDTO actualizarAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("archivo") MultipartFile archivo
    ) {
        return avatarUsuarioService.actualizarAvatar(extraerUserId(jwt), archivo);
    }

    /** Quita el avatar y vuelve a iniciales. Idempotente. */
    @DeleteMapping
    public UsuarioActualDTO eliminarAvatar(@AuthenticationPrincipal Jwt jwt) {
        return avatarUsuarioService.eliminarAvatar(extraerUserId(jwt));
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
