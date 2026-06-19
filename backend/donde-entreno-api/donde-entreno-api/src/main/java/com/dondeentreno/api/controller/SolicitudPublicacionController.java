package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.SolicitudPublicacionRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.service.SolicitudPublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de solicitudes publicas de publicacion.
 *
 * Esta clase expone el endpoint publico para recibir solicitudes
 * desde el formulario web sin login.
 */
@RestController
@RequestMapping("/api/solicitudes-publicacion")
public class SolicitudPublicacionController {

    private final SolicitudPublicacionService solicitudPublicacionService;

    /**
     * Inyeccion de dependencias por constructor.
     */
    public SolicitudPublicacionController(SolicitudPublicacionService solicitudPublicacionService) {
        this.solicitudPublicacionService = solicitudPublicacionService;
    }

    /**
     * Crea una solicitud publica de publicacion.
     *
     * @param request datos enviados desde el formulario publico.
     * @return respuesta con codigo de seguimiento y estado inicial.
     */
    @PostMapping
    public ResponseEntity<SolicitudPublicacionResponseDTO> crearSolicitud(
            @Valid @RequestBody SolicitudPublicacionRequestDTO request
    ) {
        SolicitudPublicacionResponseDTO respuesta =
                solicitudPublicacionService.crearSolicitud(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
