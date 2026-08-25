package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ConversacionDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.InboxService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * La bandeja del publicador. Bajo /api/publicador/** → exige rol
 * PUBLICADOR.
 *
 * NO hay endpoint para INICIAR una conversación: el publicador solo
 * responde. Que no pueda escribir en frío es la regla que sostiene la
 * confianza en la bandeja.
 */
@RestController
@RequestMapping("/api/publicador/consultas")
public class PublicadorConsultasController {

    private final InboxService inboxService;

    public PublicadorConsultasController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    public List<ConversacionDTO> bandeja(@AuthenticationPrincipal Jwt jwt) {
        return inboxService.bandejaDelPublicador(extraerUserId(jwt));
    }

    /** El número del badge, sin traer la bandeja entera. */
    @GetMapping("/contador")
    public Map<String, Long> contador(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("noLeidos", inboxService.contarNoLeidosDelPublicador(extraerUserId(jwt)));
    }

    @GetMapping("/{id}")
    public ConversacionDTO hilo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return inboxService.verHilo(extraerUserId(jwt), id, true);
    }

    @PostMapping("/{id}/respuestas")
    public ConversacionDTO responder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody Map<String, String> cuerpo
    ) {
        return inboxService.responder(extraerUserId(jwt), id, cuerpo.get("texto"));
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
