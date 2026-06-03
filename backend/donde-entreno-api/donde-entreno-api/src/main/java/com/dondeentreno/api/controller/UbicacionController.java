package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.UbicacionDTO;
import com.dondeentreno.api.service.UbicacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de ubicaciones.
 *
 * Esta clase expone endpoints HTTP relacionados
 * con los lugares físicos donde se realizan actividades deportivas.
 */
@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el UbicacionService
     * y lo entrega a este controller.
     */
    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    /**
     * Lista ubicaciones activas.
     *
     * Este endpoint permite varias formas de uso:
     *
     * 1) Sin filtro:
     * GET http://localhost:8080/api/ubicaciones
     *
     * 2) Filtrando por ciudad:
     * GET http://localhost:8080/api/ubicaciones?ciudadId=1
     *
     * 3) Filtrando por barrio:
     * GET http://localhost:8080/api/ubicaciones?barrioId=1
     *
     * 4) Filtrando por perfil publicador:
     * GET http://localhost:8080/api/ubicaciones?perfilPublicadorId=1
     *
     * Por ahora usamos un filtro a la vez.
     *
     * @param ciudadId ID opcional de la ciudad.
     * @param barrioId ID opcional del barrio.
     * @param perfilPublicadorId ID opcional del perfil publicador.
     * @return lista de ubicaciones activas en formato DTO.
     */
    @GetMapping
    public List<UbicacionDTO> listarUbicaciones(
            @RequestParam(required = false) Long ciudadId,
            @RequestParam(required = false) Long barrioId,
            @RequestParam(required = false) Long perfilPublicadorId
    ) {
        if (perfilPublicadorId != null) {
            return ubicacionService.obtenerUbicacionesPorPerfilPublicador(perfilPublicadorId);
        }

        if (barrioId != null) {
            return ubicacionService.obtenerUbicacionesPorBarrio(barrioId);
        }

        if (ciudadId != null) {
            return ubicacionService.obtenerUbicacionesPorCiudad(ciudadId);
        }

        return ubicacionService.obtenerUbicacionesActivas();
    }
}