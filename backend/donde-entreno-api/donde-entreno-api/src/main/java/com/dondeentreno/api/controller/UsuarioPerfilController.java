package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActualizarPerfilUsuarioRequestDTO;
import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.UsuarioPerfilService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Edición de datos del usuario (Fase 2 social): nombre y apellido,
 * inline desde /configuracion. Bajo /api/usuario/**: anónimo 401.
 */
@RestController
@RequestMapping("/api/usuario/perfil")
public class UsuarioPerfilController {

    private final UsuarioPerfilService usuarioPerfilService;

    public UsuarioPerfilController(UsuarioPerfilService usuarioPerfilService) {
        this.usuarioPerfilService = usuarioPerfilService;
    }

    @PatchMapping
    public UsuarioActualDTO actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarPerfilUsuarioRequestDTO request
    ) {
        return usuarioPerfilService.actualizarDatos(
                extraerUserId(jwt),
                request.getNombre(),
                request.getApellido()
        );
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
