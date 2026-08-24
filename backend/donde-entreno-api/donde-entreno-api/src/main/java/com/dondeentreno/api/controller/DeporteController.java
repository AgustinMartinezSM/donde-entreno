package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.service.DeporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de deportes.
 *
 * Esta clase expone endpoints HTTP relacionados
 * con los deportes de DondeEntreno.
 */
@RestController
@RequestMapping("/api/deportes")
public class DeporteController {

    /**
     * Con menos de 3 deportes con vistas, el "ranking" lo arman dos
     * clicks: la sección no se muestra (decisión 4 del plan).
     */
    private static final int MINIMO_DEPORTES_PARA_RANKING = 3;

    private final DeporteService deporteService;
    private final com.dondeentreno.api.service.InteraccionService interaccionService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el DeporteService
     * y lo entrega a este controller.
     */
    public DeporteController(
            DeporteService deporteService,
            com.dondeentreno.api.service.InteraccionService interaccionService
    ) {
        this.deporteService = deporteService;
        this.interaccionService = interaccionService;
    }

    /**
     * Deportes más vistos de los últimos N días (Fase 6), derivado del
     * tracking anónimo. Devuelve lista VACÍA si no hay señal
     * suficiente: el frontend entonces no dibuja la sección, que es
     * mejor que volver a una lista inventada.
     */
    @GetMapping("/populares")
    public List<com.dondeentreno.api.dto.DeportePopularDTO> listarPopulares(
            @RequestParam(defaultValue = "30") int dias,
            @RequestParam(defaultValue = "6") int limite
    ) {
        return interaccionService
                .deportesMasVistos(dias, MINIMO_DEPORTES_PARA_RANKING, limite)
                .stream()
                .map(fila -> new com.dondeentreno.api.dto.DeportePopularDTO(
                        (String) fila[0],
                        (String) fila[1],
                        ((Number) fila[2]).longValue()
                ))
                .toList();
    }

    /**
     * Lista deportes activos.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Sin filtro:
     * GET http://localhost:8080/api/deportes
     *
     * 2) Filtrando por categoría:
     * GET http://localhost:8080/api/deportes?categoriaSlug=deportes-de-combate
     *
     * @param categoriaSlug slug opcional de la categoría deportiva.
     * @return lista de deportes activos en formato DTO.
     */
    @GetMapping
    public List<DeporteDTO> listarDeportes(
            @RequestParam(required = false) String categoriaSlug
    ) {
        if (categoriaSlug != null && !categoriaSlug.isBlank()) {
            return deporteService.obtenerDeportesActivosPorCategoriaSlug(categoriaSlug);
        }

        return deporteService.obtenerDeportesActivos();
    }
}