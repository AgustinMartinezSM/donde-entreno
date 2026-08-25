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
    private final com.dondeentreno.api.service.InboxService inboxService;
    private final com.dondeentreno.api.service.GrupoActividadService grupoActividadService;

    public AdminModeracionSocialController(
            ValoracionService valoracionService,
            PreguntaActividadService preguntaActividadService,
            com.dondeentreno.api.service.ComentarioImagenService comentarioImagenService,
            com.dondeentreno.api.service.NovedadService novedadService,
            com.dondeentreno.api.service.EventoDeportivoService eventoDeportivoService,
            com.dondeentreno.api.service.InboxService inboxService,
            com.dondeentreno.api.service.GrupoActividadService grupoActividadService
    ) {
        this.grupoActividadService = grupoActividadService;
        this.novedadService = novedadService;
        this.eventoDeportivoService = eventoDeportivoService;
        this.inboxService = inboxService;
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

    /** Aviso de un grupo (script 38). */
    @PatchMapping("/avisos-grupo/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarAvisoGrupo(@PathVariable Long id) {
        grupoActividadService.ocultarAvisoPorAdmin(id);
    }

    /**
     * Comentario dentro de un grupo (script 38).
     *
     * Igual que en el inbox: el admin ACCIONA sobre lo reportado, pero
     * no existe ningún endpoint que le devuelva el contenido del grupo.
     */
    @PatchMapping("/comentarios-grupo/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarComentarioGrupo(@PathVariable Long id) {
        grupoActividadService.ocultarComentarioPorAdmin(id);
    }

    /** Mensaje privado reportado (inbox). */
    @PatchMapping("/mensajes/{id}/ocultar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultarMensaje(@PathVariable Long id) {
        inboxService.ocultarMensajePorAdmin(id);
    }

    /**
     * El contexto del mensaje reportado: ESE mensaje y a lo sumo los
     * dos anteriores.
     *
     * Es lo único que un admin puede ver de una conversación privada.
     * **No existe un endpoint que devuelva el hilo completo**, y no es
     * un olvido: la moderación es unipersonal y este sería el único
     * lugar del producto donde alguien lee lo que dos personas se
     * escriben en privado. Está prometido en /privacidad.
     */
    @org.springframework.web.bind.annotation.GetMapping("/mensajes/{id}/contexto")
    public java.util.List<com.dondeentreno.api.dto.MensajeDTO> contextoDeMensaje(
            @PathVariable Long id
    ) {
        return inboxService.contextoDeReporte(id);
    }
}
