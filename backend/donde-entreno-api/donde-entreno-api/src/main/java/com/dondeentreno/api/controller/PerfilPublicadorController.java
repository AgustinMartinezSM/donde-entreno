package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.dto.PreguntaActividadDTO;
import com.dondeentreno.api.dto.ResumenValoracionesDTO;
import com.dondeentreno.api.security.LimitadorInteracciones;
import com.dondeentreno.api.service.InteraccionService;
import com.dondeentreno.api.service.PerfilPublicadorService;
import com.dondeentreno.api.service.PreguntaActividadService;
import com.dondeentreno.api.service.ValoracionService;
import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.service.ImagenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller de perfiles publicadores.
 *
 * Esta clase expone endpoints HTTP relacionados con los perfiles
 * que publican actividades en DondeEntreno.
 *
 * Un perfil publicador puede ser:
 * - Club
 * - Gimnasio
 * - Profesor independiente
 * - Institución
 * - Escuela deportiva
 * - Espacio de entrenamiento
 */
@RestController
@RequestMapping("/api/perfiles-publicadores")
public class PerfilPublicadorController {

    private final PerfilPublicadorService perfilPublicadorService;
    private final ImagenService imagenService;
    private final com.dondeentreno.api.service.ActividadService actividadService;
    private final ValoracionService valoracionService;
    private final PreguntaActividadService preguntaActividadService;
    private final InteraccionService interaccionService;
    private final LimitadorInteracciones limitador;
    private final com.dondeentreno.api.service.NovedadService novedadService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los services
     * y los entrega a este controller.
     */
    public PerfilPublicadorController(
            PerfilPublicadorService perfilPublicadorService,
            ImagenService imagenService,
            com.dondeentreno.api.service.ActividadService actividadService,
            ValoracionService valoracionService,
            PreguntaActividadService preguntaActividadService,
            InteraccionService interaccionService,
            LimitadorInteracciones limitador,
            com.dondeentreno.api.service.NovedadService novedadService
    ) {
        this.novedadService = novedadService;
        this.perfilPublicadorService = perfilPublicadorService;
        this.imagenService = imagenService;
        this.actividadService = actividadService;
        this.valoracionService = valoracionService;
        this.preguntaActividadService = preguntaActividadService;
        this.interaccionService = interaccionService;
        this.limitador = limitador;
    }

    /**
     * Lista perfiles publicadores activos.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Sin filtro:
     * GET http://localhost:8080/api/perfiles-publicadores
     *
     * 2) Filtrando por tipo de publicador:
     * GET http://localhost:8080/api/perfiles-publicadores?tipoPublicador=CLUB
     *
     * Tipos válidos según la base:
     * CLUB
     * GIMNASIO
     * PROFESOR_INDEPENDIENTE
     * INSTITUCION
     * ESCUELA_DEPORTIVA
     * ESPACIO_ENTRENAMIENTO
     *
     * @param tipoPublicador tipo opcional de publicador.
     * @return lista de perfiles activos en formato DTO.
     */
    @GetMapping
    public List<PerfilPublicadorDTO> listarPerfilesPublicadores(
            @RequestParam(required = false) String tipoPublicador
    ) {
        if (tipoPublicador != null && !tipoPublicador.isBlank()) {
            return perfilPublicadorService.obtenerPerfilesActivosPorTipo(tipoPublicador);
        }

        return perfilPublicadorService.obtenerPerfilesActivos();
    }

    /**
     * Obtiene las imágenes activas de un perfil publicador.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Todas las imágenes:
     * GET http://localhost:8080/api/perfiles-publicadores/1/imagenes
     *
     * 2) Filtrando por tipo:
     * GET http://localhost:8080/api/perfiles-publicadores/1/imagenes?tipoImagen=LOGO
     *
     * Tipos posibles según la base:
     * LOGO, PORTADA, PRINCIPAL, GALERIA.
     *
     * @param id ID del perfil publicador.
     * @param tipoImagen tipo opcional de imagen.
     * @return lista de imágenes activas del perfil publicador.
     */
    /**
     * Detalle público de un perfil publicador.
     *
     * GET /api/perfiles-publicadores/{idOSlug}
     *
     * Acepta id numérico (los links y clientes viejos siguen andando)
     * o slug (script 27). Responde 404 si el perfil no existe o no
     * está activo. La sub-ruta /imagenes sigue siendo por id: el DTO
     * del detalle trae el id para encadenarla.
     */
    @GetMapping("/{idOSlug}")
    public PerfilPublicadorDTO obtenerPerfilPublicador(@PathVariable String idOSlug) {
        return perfilPublicadorService.obtenerPerfilActivoPorIdOSlug(idOSlug);
    }

    @GetMapping("/{id}/imagenes")
    public List<ImagenDTO> obtenerImagenesPorPerfilPublicador(
            @PathVariable Long id,
            @RequestParam(required = false) String tipoImagen
    ) {
        if (tipoImagen != null && !tipoImagen.isBlank()) {
            return imagenService.obtenerImagenesPorPerfilPublicadorYTipo(id, tipoImagen);
        }

        return imagenService.obtenerImagenesPorPerfilPublicador(id);
    }

    /**
     * TODAS las fotos visibles del publicador (Fase 5): las del perfil
     * y las de sus actividades, en UN request. Antes el frontend hacía
     * una llamada por actividad para armar esta misma grilla.
     */
    @GetMapping("/{id}/fotos")
    public List<ImagenDTO> obtenerFotosDelPublicador(@PathVariable Long id) {
        return imagenService.obtenerFotosVisiblesDePublicador(id);
    }

    /**
     * Novedades del canal (Fase 8): lo que el publicador contó, sin
     * necesidad de crear o editar una actividad.
     */
    @GetMapping("/{id}/novedades")
    public List<com.dondeentreno.api.dto.NovedadDTO> obtenerNovedades(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limite
    ) {
        return novedadService.listarPublicasDe(id, limite);
    }

    /**
     * Las destacadas del publicador (Fase 5): hasta 3, en su orden.
     * Van arriba del listado normal en el perfil público.
     */
    @GetMapping("/{id}/destacadas")
    public List<com.dondeentreno.api.dto.ActividadDTO> obtenerDestacadas(@PathVariable Long id) {
        return actividadService.obtenerDestacadasDePerfil(id);
    }

    /**
     * Opiniones del publicador (Fase 5): las valoraciones de TODAS sus
     * actividades juntas, con el resumen. JWT opcional — con sesión,
     * cada reseña sabe si es propia.
     */
    @GetMapping("/{idOSlug}/valoraciones")
    public ResumenValoracionesDTO obtenerValoracionesDelPublicador(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String idOSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long perfilId = perfilPublicadorService.obtenerPerfilActivoPorIdOSlug(idOSlug).getId();

        return valoracionService.resumenDePublicador(
                perfilId,
                extraerUserIdOpcional(jwt),
                page,
                size
        );
    }

    /**
     * Preguntas ya respondidas del publicador (Fase 5). Solo las
     * respondidas: en la vidriera del publicador una pregunta sin
     * responder juega en contra, y en el detalle se ven todas.
     */
    @GetMapping("/{idOSlug}/preguntas")
    public List<PreguntaActividadDTO> obtenerPreguntasDelPublicador(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String idOSlug,
            @RequestParam(defaultValue = "10") int limite
    ) {
        Long perfilId = perfilPublicadorService.obtenerPerfilActivoPorIdOSlug(idOSlug).getId();

        return preguntaActividadService.listarRespondidasDePublicador(
                perfilId,
                extraerUserIdOpcional(jwt),
                limite
        );
    }

    /**
     * Interacción anónima sobre el perfil (Fase 5): hoy el WhatsApp del
     * perfil no se mide. Responde 204 y nunca falla hacia el cliente:
     * un beacon roto no puede romper la página.
     */
    @PostMapping("/{id}/interacciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarInteraccionDePerfil(
            @PathVariable Long id,
            @RequestBody Map<String, String> cuerpo,
            HttpServletRequest peticion
    ) {
        if (!limitador.registrar(LimitadorInteracciones.identificadorDe(peticion))) {
            /* Excedido: se ignora en silencio, igual que en actividades. */
            return;
        }

        interaccionService.registrarEnPerfil(id, cuerpo.get("tipo"));
    }

    private Long extraerUserIdOpcional(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        Object userId = jwt.getClaim("userId");

        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String texto) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException excepcion) {
                return null;
            }
        }

        return null;
    }
}