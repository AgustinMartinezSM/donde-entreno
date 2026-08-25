package com.dondeentreno.api.controller;

import com.dondeentreno.api.service.PreguntaActividadService;
import com.dondeentreno.api.service.ValoracionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderación flexible del contenido social (script 29): el admin
 * OCULTA (no borra — queda el rastro). El acceso lo corta
 * SecurityConfig: /api/admin/** exige ADMIN o SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminModeracionSocialController {

    private final ValoracionService valoracionService;
    private final PreguntaActividadService preguntaActividadService;
    private final com.dondeentreno.api.service.ComentarioImagenService comentarioImagenService;
    private final com.dondeentreno.api.service.NovedadService novedadService;
    private final com.dondeentreno.api.service.EventoDeportivoService eventoDeportivoService;

    public AdminModeracionSocialController(
            ValoracionService valoracionService,
            PreguntaActividadService preguntaActividadService,
            com.dondeentreno.api.service.ComentarioImagenService comentarioImagenService,
            com.dondeentreno.api.service.NovedadService novedadService,
            com.dondeentreno.api.service.EventoDeportivoService eventoDeportivoService
    ) {
        this.novedadService = novedadService;
        this.eventoDeportivoService = eventoDeportivoService;
        this.valoracionService = valoracionService;
        this.preguntaActividadService = preguntaActividadService;
        this.comentarioImagenService = comentarioImagenService;
    }

    @PatchMapping("/valoraciones/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarValoracion(@PathVariable Long id) {
        valoracionService.ocultarPorAdmin(id);
    }

    @PatchMapping("/preguntas/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarPregunta(@PathVariable Long id) {
        preguntaActividadService.ocultarPorAdmin(id);
    }

    @PatchMapping("/comentarios/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarComentario(@PathVariable Long id) {
        comentarioImagenService.ocultarPorAdmin(id);
    }

    /** Novedad del canal del publicador (Fase 8). */
    @PatchMapping("/novedades/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarNovedad(@PathVariable Long id) {
        novedadService.ocultarPorAdmin(id);
    }

    /** Evento deportivo (Fase 9). */
    @PatchMapping("/eventos/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarEvento(@PathVariable Long id) {
        eventoDeportivoService.ocultarPorAdmin(id);
    }
}
