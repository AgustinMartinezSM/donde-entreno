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

    private final DeporteService deporteService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el DeporteService
     * y lo entrega a este controller.
     */
    public DeporteController(DeporteService deporteService) {
        this.deporteService = deporteService;
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