package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAprobacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionCambiarEstadoRequestDTO;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.service.SolicitudPublicacionAdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller admin para revisar solicitudes de publicacion.
 */
@RestController
@RequestMapping("/api/admin/solicitudes-publicacion")
public class SolicitudPublicacionAdminController {

    private final SolicitudPublicacionAdminService solicitudPublicacionAdminService;

    public SolicitudPublicacionAdminController(
            SolicitudPublicacionAdminService solicitudPublicacionAdminService
    ) {
        this.solicitudPublicacionAdminService = solicitudPublicacionAdminService;
    }

    @GetMapping
    public PaginaResponseDTO<SolicitudPublicacionAdminResumenDTO> listarSolicitudes(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recientes") String orden
    ) {
        return solicitudPublicacionAdminService.listarSolicitudes(estado, page, size, orden);
    }

    @GetMapping("/{id}")
    public SolicitudPublicacionAdminDetalleDTO obtenerDetalle(@PathVariable Long id) {
        return solicitudPublicacionAdminService.obtenerDetalle(id);
    }

    @PatchMapping("/{id}/estado")
    public SolicitudPublicacionAdminDetalleDTO cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudPublicacionCambiarEstadoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioAutenticadoId = extraerUserId(jwt);
        return solicitudPublicacionAdminService.cambiarEstado(id, request, usuarioAutenticadoId);
    }

    @PostMapping("/{id}/aprobar")
    public SolicitudPublicacionAprobacionResponseDTO aprobarSolicitud(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioAutenticadoId = extraerUserId(jwt);
        return solicitudPublicacionAdminService.aprobarSolicitud(id, usuarioAutenticadoId);
    }

    private Long extraerUserId(Jwt jwt) {
        if (jwt == null) {
            throw new SolicitudPublicacionInvalidaException("Usuario autenticado invalido.");
        }

        Object userId = jwt.getClaims().get("userId");

        if (userId instanceof Number numero) {
            return numero.longValue();
        }

        if (userId instanceof String texto && !texto.isBlank()) {
            try {
                return Long.valueOf(texto);
            } catch (NumberFormatException exception) {
                throw new SolicitudPublicacionInvalidaException("Usuario autenticado invalido.");
            }
        }

        throw new SolicitudPublicacionInvalidaException("Usuario autenticado invalido.");
    }
}
