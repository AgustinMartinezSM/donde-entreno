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

    public AdminModeracionSocialController(
            ValoracionService valoracionService,
            PreguntaActividadService preguntaActividadService
    ) {
        this.valoracionService = valoracionService;
        this.preguntaActividadService = preguntaActividadService;
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
}
