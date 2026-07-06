package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PerfilPublicadorActualDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorResumenDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.PublicadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del panel publicador autenticado.
 */
@RestController
@RequestMapping("/api/publicador")
public class PublicadorController {

    private final PublicadorService publicadorService;

    public PublicadorController(PublicadorService publicadorService) {
        this.publicadorService = publicadorService;
    }

    @GetMapping("/me")
    public PerfilPublicadorActualDTO obtenerMiPerfil(@AuthenticationPrincipal Jwt jwt) {
        return publicadorService.obtenerMiPerfil(extraerUserId(jwt));
    }

    @GetMapping("/solicitudes")
    public PaginaResponseDTO<SolicitudPublicadorResumenDTO> listarMisSolicitudes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orden
    ) {
        return publicadorService.listarMisSolicitudes(extraerUserId(jwt), estado, page, size, orden);
    }

    @GetMapping("/solicitudes/{id}")
    public SolicitudPublicadorDetalleDTO obtenerMiSolicitud(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return publicadorService.obtenerMiSolicitud(extraerUserId(jwt), id);
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudPublicacionResponseDTO> crearMiSolicitud(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SolicitudPublicadorRequestDTO request
    ) {
        SolicitudPublicacionResponseDTO respuesta =
                publicadorService.crearMiSolicitud(extraerUserId(jwt), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
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
