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

    private final ActividadService actividadService;
    private final HorarioActividadService horarioActividadService;
    private final ImagenService imagenService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los services
     * y los entrega a este controller.
     */
    public ActividadController(
            ActividadService actividadService,
            HorarioActividadService horarioActividadService,
            ImagenService imagenService
    ) {
        this.actividadService = actividadService;
        this.horarioActividadService = horarioActividadService;
        this.imagenService = imagenService;
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
