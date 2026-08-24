package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.UbicacionDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.PublicadorUbicacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Sedes del publicador autenticado (Fase 7): listar y cargar el punto
 * exacto. Bajo /api/publicador/** → exige rol PUBLICADOR.
 */
@RestController
@RequestMapping("/api/publicador/ubicaciones")
public class PublicadorUbicacionController {

    private final PublicadorUbicacionService publicadorUbicacionService;

    public PublicadorUbicacionController(
            PublicadorUbicacionService publicadorUbicacionService
    ) {
        this.publicadorUbicacionService = publicadorUbicacionService;
    }

    @GetMapping
    public List<UbicacionDTO> listarMisUbicaciones(@AuthenticationPrincipal Jwt jwt) {
        return publicadorUbicacionService.listarMisUbicaciones(extraerUserId(jwt));
    }

    /**
     * Guarda el punto de una sede propia. El cuerpo trae lo que la
     * persona PEGÓ (link de Google Maps o coordenadas): el backend lo
     * interpreta, así admin y publicador comparten la misma lógica.
     */
    @PatchMapping("/{id}/coordenadas")
    public UbicacionDTO guardarCoordenadas(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody Map<String, String> cuerpo
    ) {
        return publicadorUbicacionService.guardarCoordenadas(
                extraerUserId(jwt),
                id,
                cuerpo.get("pegado")
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
