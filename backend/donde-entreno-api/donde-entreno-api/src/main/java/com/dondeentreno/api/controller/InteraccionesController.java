package com.dondeentreno.api.controller;

import com.dondeentreno.api.security.LimitadorInteracciones;
import com.dondeentreno.api.service.InteraccionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tracking anónimo de interacciones (script 28, Fase 2 social).
 * PÚBLICO a propósito: los visitantes también cuentan. El rate limit
 * por IP absorbe el abuso barato; superarlo NO da error — el evento
 * simplemente se ignora (un beacon jamás debe romper la navegación).
 */
@RestController
@RequestMapping("/api/actividades/{actividadId}/interacciones")
public class InteraccionesController {

    private static final int MAX_LARGO_IP = 60;

    private final InteraccionService interaccionService;
    private final LimitadorInteracciones limitador;

    public InteraccionesController(
            InteraccionService interaccionService,
            LimitadorInteracciones limitador
    ) {
        this.interaccionService = interaccionService;
        this.limitador = limitador;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrar(
            @PathVariable Long actividadId,
            @RequestBody Map<String, String> cuerpo,
            HttpServletRequest peticion
    ) {
        if (!limitador.registrar(obtenerIdentificadorCliente(peticion))) {
            return;
        }

        interaccionService.registrar(actividadId, cuerpo.get("tipo"));
    }

    /* Mismo criterio que AsistenteController: detrás del proxy de
       Render la IP real viaja en X-Forwarded-For. */
    private String obtenerIdentificadorCliente(HttpServletRequest peticion) {
        String reenviadas = peticion.getHeader("X-Forwarded-For");

        if (reenviadas != null && !reenviadas.isBlank()) {
            String primera = reenviadas.split(",")[0].trim();

            if (!primera.isEmpty()) {
                return recortar(primera);
            }
        }

        String remota = peticion.getRemoteAddr();

        return remota == null ? "desconocida" : recortar(remota);
    }

    private String recortar(String valor) {
        return valor.length() <= MAX_LARGO_IP
                ? valor
                : valor.substring(0, MAX_LARGO_IP);
    }
}
