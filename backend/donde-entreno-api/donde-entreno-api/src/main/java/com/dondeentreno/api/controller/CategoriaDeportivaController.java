package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.service.CategoriaDeportivaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller de categorías deportivas.
 *
 * Esta clase expone endpoints HTTP relacionados
 * con las categorías deportivas.
 */
@RestController
@RequestMapping("/api/categorias-deportivas")
public class CategoriaDeportivaController {

    private final CategoriaDeportivaService categoriaDeportivaService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el service y lo inyecta acá.
     */
    public CategoriaDeportivaController(CategoriaDeportivaService categoriaDeportivaService) {
        this.categoriaDeportivaService = categoriaDeportivaService;
    }

    /**
     * Endpoint para listar categorías deportivas activas.
     *
     * URL:
     * GET http://localhost:8080/api/categorias-deportivas
     *
     * @return lista de categorías deportivas activas en formato DTO.
     */
    @GetMapping
    public List<CategoriaDeportivaDTO> listarCategoriasActivas() {
        return categoriaDeportivaService.obtenerCategoriasActivas();
    }
}