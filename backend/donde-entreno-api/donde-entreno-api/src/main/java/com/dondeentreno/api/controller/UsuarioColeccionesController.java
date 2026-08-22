package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ColeccionGuardadosDTO;
import com.dondeentreno.api.dto.NombreColeccionRequestDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.ColeccionesGuardadosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Colecciones de guardados (script 22, bloque 13). Bajo /api/usuario/**:
 * anonimo recibe 401 y todo se acota al usuario del JWT.
 */
@RestController
@RequestMapping("/api/usuario/colecciones")
public class UsuarioColeccionesController {

    private final ColeccionesGuardadosService coleccionesGuardadosService;

    public UsuarioColeccionesController(
            ColeccionesGuardadosService coleccionesGuardadosService
    ) {
        this.coleccionesGuardadosService = coleccionesGuardadosService;
    }

    @GetMapping
    public List<ColeccionGuardadosDTO> listar(@AuthenticationPrincipal Jwt jwt) {
        return coleccionesGuardadosService.listar(extraerUserId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColeccionGuardadosDTO crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NombreColeccionRequestDTO request
    ) {
        return coleccionesGuardadosService.crear(extraerUserId(jwt), request.getNombre());
    }

    @PatchMapping("/{id}")
    public ColeccionGuardadosDTO renombrar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody NombreColeccionRequestDTO request
    ) {
        return coleccionesGuardadosService.renombrar(
                extraerUserId(jwt),
                id,
                request.getNombre()
        );
    }

    /** Borra la coleccion; sus guardados vuelven a "Todos" (SET NULL). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        coleccionesGuardadosService.eliminar(extraerUserId(jwt), id);
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
