package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.service.CiudadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de ciudades.
 *
 * Esta clase expone endpoints HTTP relacionados
 * con las ciudades disponibles en DondeEntreno.
 */
@RestController
@RequestMapping("/api/ciudades")
public class CiudadController {

    private final CiudadService ciudadService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el CiudadService
     * y lo entrega a este controller.
     */
    public CiudadController(CiudadService ciudadService) {
        this.ciudadService = ciudadService;
    }

    /**
     * Lista ciudades activas.
     *
     * URL:
     * GET http://localhost:8080/api/ciudades
     *
     * @return lista de ciudades activas en formato DTO.
     */
    @GetMapping
    public List<CiudadDTO> listarCiudadesActivas() {
        return ciudadService.obtenerCiudadesActivas();
    }

    /**
     * Obtiene una ciudad activa por slug.
     *
     * URL:
     * GET http://localhost:8080/api/ciudades/mar-del-plata
     *
     * @param slug slug publico de la ciudad.
     * @return ciudad activa en formato DTO.
     */
    @GetMapping("/{slug}")
    public CiudadDTO obtenerCiudadActivaPorSlug(@PathVariable String slug) {
        return ciudadService.obtenerCiudadActivaPorSlug(slug);
    }
}
