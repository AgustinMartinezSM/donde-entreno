package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudCambioActividadRequestDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.dto.SolicitudCambioResumenDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.SolicitudCambioActividadService;
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
 * Endpoints del publicador para solicitudes de cambio sobre
 * actividades publicadas propias.
 */
@RestController
@RequestMapping("/api/publicador")
public class SolicitudCambioActividadController {

    private final SolicitudCambioActividadService solicitudCambioService;

    public SolicitudCambioActividadController(
            SolicitudCambioActividadService solicitudCambioService
    ) {
        this.solicitudCambioService = solicitudCambioService;
    }

    @PostMapping("/actividades/{actividadId}/solicitudes-cambio")
    public ResponseEntity<SolicitudCambioDetalleDTO> crearSolicitudCambio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @Valid @RequestBody SolicitudCambioActividadRequestDTO request
    ) {
        SolicitudCambioDetalleDTO respuesta = solicitudCambioService.crearSolicitud(
                extraerUserId(jwt),
                actividadId,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/solicitudes-cambio")
    public PaginaResponseDTO<SolicitudCambioResumenDTO> listarMisSolicitudesCambio(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orden
    ) {
        return solicitudCambioService.listarMias(extraerUserId(jwt), estado, page, size, orden);
    }

    @GetMapping("/solicitudes-cambio/{id}")
    public SolicitudCambioDetalleDTO obtenerMiSolicitudCambio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return solicitudCambioService.obtenerMia(extraerUserId(jwt), id);
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
