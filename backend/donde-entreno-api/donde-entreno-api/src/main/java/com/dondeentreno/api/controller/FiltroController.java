package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import com.dondeentreno.api.service.FiltroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de filtros.
 *
 * Expone endpoints relacionados con las opciones
 * necesarias para construir el buscador del frontend.
 */
@RestController
@RequestMapping("/api/filtros")
public class FiltroController {

    private final FiltroService filtroService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente FiltroService
     * y lo entrega a este controller.
     */
    public FiltroController(FiltroService filtroService) {
        this.filtroService = filtroService;
    }

    /**
     * Devuelve todas las opciones disponibles para filtros.
     *
     * Ejemplo:
     * GET http://localhost:8080/api/filtros/opciones
     *
     * @return opciones para armar el buscador.
     */
    @GetMapping("/opciones")
    public FiltroOpcionesDTO obtenerOpcionesDeFiltros() {
        return filtroService.obtenerOpcionesDeFiltros();
    }
}
