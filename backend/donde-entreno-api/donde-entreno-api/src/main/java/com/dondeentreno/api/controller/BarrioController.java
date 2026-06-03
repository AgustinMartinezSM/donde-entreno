package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.service.BarrioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de barrios.
 *
 * Esta clase expone endpoints HTTP relacionados
 * con los barrios o zonas disponibles en DondeEntreno.
 */
@RestController
@RequestMapping("/api/barrios")
public class BarrioController {

    private final BarrioService barrioService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el BarrioService
     * y lo entrega a este controller.
     */
    public BarrioController(BarrioService barrioService) {
        this.barrioService = barrioService;
    }

    /**
     * Lista barrios activos.
     *
     * Este endpoint permite dos formas de uso:
     *
     * 1) Sin filtro:
     * GET http://localhost:8080/api/barrios
     *
     * 2) Filtrando por ciudad:
     * GET http://localhost:8080/api/barrios?ciudadId=1
     *
     * @param ciudadId ID opcional de la ciudad.
     * @return lista de barrios activos en formato DTO.
     */
    @GetMapping
    public List<BarrioDTO> listarBarrios(
            @RequestParam(required = false) Long ciudadId
    ) {
        if (ciudadId != null) {
            return barrioService.obtenerBarriosActivosPorCiudad(ciudadId);
        }

        return barrioService.obtenerBarriosActivos();
    }
}