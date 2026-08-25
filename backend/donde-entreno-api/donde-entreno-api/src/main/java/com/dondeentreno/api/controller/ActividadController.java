package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.service.ActividadService;
import com.dondeentreno.api.dto.HorarioActividadDTO;
import com.dondeentreno.api.service.HorarioActividadService;
import com.dondeentreno.api.dto.ActividadDetalleDTO;
import com.dondeentreno.api.dto.ImagenDTO;
import com.dondeentreno.api.service.ImagenService;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de actividades.
 *
 * Esta clase expone endpoints HTTP relacionados
 * con las actividades deportivas publicadas en DondeEntreno.
 *
 * Las actividades son el recurso principal del MVP,
 * porque representan lo que el visitante busca:
 * boxeo, fútbol, yoga, funcional, jiu jitsu, natación, etc.
 */
@RestController
@RequestMapping("/api/actividades")
public class ActividadController {

    /* Con menos de tres actividades con señal, el ranking miente. */
    private static final int MINIMO_ACTIVIDADES_PARA_RANKING = 3;

    private final ActividadService actividadService;
    private final HorarioActividadService horarioActividadService;
    private final ImagenService imagenService;
    private final com.dondeentreno.api.service.InteraccionService interaccionService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los services
     * y los entrega a este controller.
     */
    public ActividadController(
            ActividadService actividadService,
            HorarioActividadService horarioActividadService,
            ImagenService imagenService,
            com.dondeentreno.api.service.InteraccionService interaccionService
    ) {
        this.actividadService = actividadService;
        this.horarioActividadService = horarioActividadService;
        this.imagenService = imagenService;
        this.interaccionService = interaccionService;
    }

    /**
     * Lista actividades activas y publicadas.
     *
     * Permite filtros combinados, búsqueda por texto y paginación.
     *
     * Ejemplos:
     *
     * GET http://localhost:8080/api/actividades
     *
     * GET http://localhost:8080/api/actividades?page=0&size=10
     *
     * GET http://localhost:8080/api/actividades?texto=boxeo&page=0&size=5
     *
     * GET http://localhost:8080/api/actividades?ciudadId=1&deporteSlug=boxeo&page=0&size=10
     *
     * @param deporteId ID opcional del deporte.
     * @param deporteSlug slug opcional del deporte.
     * @param ciudadId ID opcional de la ciudad.
     * @param ciudadSlug slug opcional de la ciudad.
     * @param barrioId ID opcional del barrio.
     * @param perfilPublicadorId ID opcional del perfil publicador.
     * @param nivel nivel opcional de la actividad.
     * @param modalidad modalidad opcional de la actividad.
     * @param texto texto libre opcional de búsqueda.
     * @param page número de página. Arranca en 0.
     * @param size cantidad de elementos por página.
     * @return página de actividades publicadas en formato DTO.
     */
    /**
     * Zonas con actividad real (Fase 7): barrios ordenados por cuántas
     * actividades publicadas tienen.
     *
     * Vive acá y no en /api/barrios porque lo que se cuenta son
     * ACTIVIDADES; el barrio es el agrupador. Meterlo en el controller
     * de barrios habría obligado a que ese recurso dependiera del
     * service de actividades.
     */
    @GetMapping("/zonas")
    public List<com.dondeentreno.api.dto.ZonaBarrioDTO> listarZonas(
            @RequestParam(required = false) String ciudadSlug
    ) {
        return actividadService.obtenerZonasPorBarrio(ciudadSlug);
    }

    /**
     * Actividades más vistas de los últimos N días (Fase 10).
     *
     * Público y derivado del tracking anónimo. Devuelve lista VACÍA si
     * no hay señal suficiente: el frontend entonces NO dibuja la
     * sección. Con poco volumen, un "lo más visto" que se arma con tres
     * clicks enseña a desconfiar de los números del sitio.
     *
     * El default es 7 días —la ventana semanal que pedía la fase— y no
     * los 30 de la fila de deportes populares: son dos preguntas
     * distintas ("qué se mira siempre" y "qué se mira ahora").
     */
    @GetMapping("/mas-vistas")
    public java.util.List<ActividadDTO> listarMasVistas(
            @RequestParam(defaultValue = "7") int dias,
            @RequestParam(defaultValue = "6") int limite
    ) {
        return interaccionService
                .actividadesMasVistas(dias, MINIMO_ACTIVIDADES_PARA_RANKING, limite)
                .stream()
                .map(com.dondeentreno.api.mapper.ActividadMapper::toDTO)
                .toList();
    }

    /**
     * Modo "cerca mío" (Fase 7): actividades ordenadas por distancia
     * al punto que manda el navegador.
     *
     * La ubicación llega por query y NO se persiste en ningún lado —
     * es lo que ya promete /privacidad. Tampoco se loguea.
     */
    @GetMapping("/cerca")
    public com.dondeentreno.api.dto.BusquedaCercaniaDTO buscarCerca(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int radioKm,
            @RequestParam(required = false) String ciudadSlug,
            @RequestParam(required = false) String deporteSlug,
            @RequestParam(defaultValue = "12") int limite
    ) {
        return actividadService.buscarCerca(
                lat,
                lng,
                radioKm,
                ciudadSlug,
                deporteSlug,
                limite
        );
    }

    @GetMapping
    public PaginaResponseDTO<ActividadDTO> listarActividades(
            @RequestParam(required = false) Long deporteId,
            @RequestParam(required = false) String deporteSlug,
            @RequestParam(required = false) Long ciudadId,
            @RequestParam(required = false) String ciudadSlug,
            @RequestParam(required = false) Long barrioId,
            @RequestParam(required = false) Long perfilPublicadorId,
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recientes") String orden
    ) {
        return actividadService.buscarActividadesConFiltrosPaginado(
                deporteId,
                deporteSlug,
                ciudadId,
                ciudadSlug,
                barrioId,
                perfilPublicadorId,
                nivel,
                modalidad,
                texto,
                page,
                size,
                orden
        );
    }

    /**
     * Obtiene el detalle público de una actividad por su slug.
     *
     * Ejemplo:
     * GET http://localhost:8080/api/actividades/boxeo-recreativo-para-adultos-principiantes
     *
     * @param slug slug único de la actividad.
     * @return detalle de la actividad en formato DTO.
     */
    @GetMapping("/{slug}")
    public ActividadDTO obtenerActividadPorSlug(@PathVariable String slug) {
        return actividadService.obtenerActividadPublicadaPorSlug(slug);
    }

    /**
     * Obtiene los horarios activos de una actividad por su slug.
     *
     * Ejemplo:
     * GET http://localhost:8080/api/actividades/boxeo-recreativo-adultos-principiantes/horarios
     *
     * @param slug slug único de la actividad.
     * @return lista de horarios activos de la actividad.
     */
    @GetMapping("/{slug}/horarios")
    public List<HorarioActividadDTO> obtenerHorariosPorActividadSlug(@PathVariable String slug) {
        return horarioActividadService.obtenerHorariosPorActividadSlug(slug);
    }

    /**
     * Obtiene las imágenes activas de una actividad por su slug.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Todas las imágenes:
     * GET http://localhost:8080/api/actividades/boxeo-recreativo-adultos-principiantes/imagenes
     *
     * 2) Filtrando por tipo:
     * GET http://localhost:8080/api/actividades/boxeo-recreativo-adultos-principiantes/imagenes?tipoImagen=PRINCIPAL
     *
     * Tipos posibles según la base:
     * LOGO, PORTADA, PRINCIPAL, GALERIA.
     *
     * @param slug slug único de la actividad.
     * @param tipoImagen tipo opcional de imagen.
     * @return lista de imágenes activas de la actividad.
     */
    @GetMapping("/{slug}/imagenes")
    public List<ImagenDTO> obtenerImagenesPorActividadSlug(
            @PathVariable String slug,
            @RequestParam(required = false) String tipoImagen
    ) {
        if (tipoImagen != null && !tipoImagen.isBlank()) {
            return imagenService.obtenerImagenesPorActividadSlugYTipo(slug, tipoImagen);
        }

        return imagenService.obtenerImagenesPorActividadSlug(slug);
    }

    /**
     * Obtiene el detalle completo de una actividad por su slug.
     *
     * Incluye:
     * - Datos principales de la actividad
     * - Horarios activos
     * - Imágenes activas
     *
     * Ejemplo:
     * GET http://localhost:8080/api/actividades/boxeo-recreativo-adultos-principiantes/detalle
     *
     * @param slug slug único de la actividad.
     * @return detalle completo de la actividad.
     */
    @GetMapping("/{slug}/detalle")
    public ActividadDetalleDTO obtenerDetalleCompletoPorSlug(@PathVariable String slug) {
        return actividadService.obtenerDetalleCompletoPorSlug(slug);
    }

}
