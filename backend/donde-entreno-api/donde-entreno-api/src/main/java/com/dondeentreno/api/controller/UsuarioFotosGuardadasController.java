package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.FotoGuardadaService;
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
 * Fotos guardadas del usuario autenticado (script 30, patrón
 * likes-fotos). Bajo /api/usuario/**: anónimo recibe 401.
 */
@RestController
@RequestMapping("/api/usuario/fotos-guardadas")
public class UsuarioFotosGuardadasController {

    private final FotoGuardadaService fotoGuardadaService;

    public UsuarioFotosGuardadasController(FotoGuardadaService fotoGuardadaService) {
        this.fotoGuardadaService = fotoGuardadaService;
    }

    /** Ids guardados, para pintar los bookmarks. */
    @GetMapping
    public List<Long> listarIds(@AuthenticationPrincipal Jwt jwt) {
        return fotoGuardadaService.listarIds(extraerUserId(jwt));
    }

    /** Las fotos guardadas visibles, para el bloque de Guardados. */
    @GetMapping("/detalle")
    public List<ImagenDTO> listarVisibles(@AuthenticationPrincipal Jwt jwt) {
        return fotoGuardadaService.listarVisibles(extraerUserId(jwt));
    }

    @PutMapping("/{imagenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long imagenId) {
        fotoGuardadaService.guardar(extraerUserId(jwt), imagenId);
    }

    @DeleteMapping("/{imagenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long imagenId) {
        fotoGuardadaService.quitar(extraerUserId(jwt), imagenId);
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
