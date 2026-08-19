package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.DeportesPreferidosRequestDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.DeportesPreferidosService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Deportes preferidos de la cuenta (sync, script 20).
 *
 * El PUT devuelve la lista efectivamente guardada (ya filtrada contra el
 * catalogo): el cliente pisa su estado local con esto y queda alineado
 * sin un GET extra.
 */
@RestController
@RequestMapping("/api/usuario/deportes")
public class UsuarioDeportesController {

    private final DeportesPreferidosService deportesPreferidosService;

    public UsuarioDeportesController(DeportesPreferidosService deportesPreferidosService) {
        this.deportesPreferidosService = deportesPreferidosService;
    }

    @GetMapping
    public List<String> listar(@AuthenticationPrincipal Jwt jwt) {
        return deportesPreferidosService.listar(extraerUserId(jwt));
    }

    @PutMapping
    public List<String> reemplazar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeportesPreferidosRequestDTO request
    ) {
        return deportesPreferidosService.reemplazar(extraerUserId(jwt), request.getSlugs());
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
