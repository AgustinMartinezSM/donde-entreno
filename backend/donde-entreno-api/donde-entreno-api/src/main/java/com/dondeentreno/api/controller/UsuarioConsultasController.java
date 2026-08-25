package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ConversacionDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.InboxService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * La bandeja del usuario. Bajo /api/usuario/** → exige sesión.
 */
@RestController
@RequestMapping("/api/usuario/consultas")
public class UsuarioConsultasController {

    private final InboxService inboxService;

    public UsuarioConsultasController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    public List<ConversacionDTO> bandeja(@AuthenticationPrincipal Jwt jwt) {
        return inboxService.bandejaDelUsuario(extraerUserId(jwt));
    }

    /** El número del badge, sin traer la bandeja entera. */
    @GetMapping("/contador")
    public Map<String, Long> contador(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("noLeidos", inboxService.contarNoLeidosDelUsuario(extraerUserId(jwt)));
    }

    @GetMapping("/{id}")
    public ConversacionDTO hilo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return inboxService.verHilo(extraerUserId(jwt), id, false);
    }

    /**
     * Consultar. Si ya existía un hilo con ese publicador por esa
     * actividad, el mensaje va ahí en vez de abrir otro.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversacionDTO consultar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> cuerpo
    ) {
        return inboxService.consultar(
                extraerUserId(jwt),
                numero(cuerpo.get("perfilPublicadorId")),
                numero(cuerpo.get("actividadId")),
                cuerpo.get("texto")
        );
    }

    /** Cerrar es solo del usuario: el publicador no puede seguir. */
    @PatchMapping("/{id}/cerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        inboxService.cerrar(extraerUserId(jwt), id);
    }

    private Long numero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException excepcion) {
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
            } catch (NumberFormatException excepcion) {
                throw new CredencialesInvalidasException("No autenticado.");
            }
        }

        throw new CredencialesInvalidasException("No autenticado.");
    }
}
