package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActualizarEstadoSolicitudCambioRequestDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.dto.SolicitudCambioResumenDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.SolicitudCambioActividadAdminService;
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
 * Endpoints administrativos para revisar y resolver solicitudes de
 * cambio de actividades publicadas.
 */
@RestController
@RequestMapping("/api/admin/solicitudes-cambio")
public class SolicitudCambioActividadAdminController {

    private final SolicitudCambioActividadAdminService adminService;

    public SolicitudCambioActividadAdminController(
            SolicitudCambioActividadAdminService adminService
    ) {
        this.adminService = adminService;
    }

    @GetMapping
    public PaginaResponseDTO<SolicitudCambioResumenDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orden
    ) {
        return adminService.listar(estado, page, size, orden);
    }

    @GetMapping("/{id}")
    public SolicitudCambioDetalleDTO obtenerDetalle(@PathVariable Long id) {
        return adminService.obtenerDetalle(id);
    }

    @PatchMapping("/{id}/estado")
    public SolicitudCambioDetalleDTO actualizarEstado(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoSolicitudCambioRequestDTO request
    ) {
        return adminService.actualizarEstado(id, request, extraerUserId(jwt));
    }

    @PostMapping("/{id}/aprobar")
    public SolicitudCambioDetalleDTO aprobar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return adminService.aprobar(id, extraerUserId(jwt));
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
