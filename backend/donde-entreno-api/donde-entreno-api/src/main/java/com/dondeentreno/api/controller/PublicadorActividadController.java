package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.CambiarVisibilidadRequestDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.PublicadorActividadDetalleDTO;
import com.dondeentreno.api.dto.PublicadorActividadResumenDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.PublicadorActividadService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de actividades reales del publicador autenticado.
 */
@RestController
@RequestMapping("/api/publicador/actividades")
public class PublicadorActividadController {

    private final PublicadorActividadService publicadorActividadService;

    public PublicadorActividadController(PublicadorActividadService publicadorActividadService) {
        this.publicadorActividadService = publicadorActividadService;
    }

    @GetMapping
    public PaginaResponseDTO<PublicadorActividadResumenDTO> listarMisActividades(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recientes") String orden
    ) {
        return publicadorActividadService.listarMisActividades(
                extraerUserId(jwt),
                page,
                size,
                orden
        );
    }

    @GetMapping("/{id}")
    public PublicadorActividadDetalleDTO obtenerMiActividad(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return publicadorActividadService.obtenerMiActividad(extraerUserId(jwt), id);
    }

    /**
     * Pausa (visible=false) o reanuda (visible=true) una actividad
     * propia (fase 6). Devuelve el detalle actualizado.
     */
    @PatchMapping("/{id}/visibilidad")
    public PublicadorActividadDetalleDTO cambiarVisibilidad(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody CambiarVisibilidadRequestDTO request
    ) {
        return publicadorActividadService.cambiarVisibilidad(
                extraerUserId(jwt),
                id,
                request.getVisible()
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
