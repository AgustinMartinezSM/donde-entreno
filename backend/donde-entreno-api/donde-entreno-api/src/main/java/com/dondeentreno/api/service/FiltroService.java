package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de filtros.
 *
 * Esta clase agrupa las opciones necesarias
 * para construir el buscador del frontend.
 */
@Service
public class FiltroService {

    private final CategoriaDeportivaService categoriaDeportivaService;
    private final DeporteService deporteService;
    private final CiudadService ciudadService;
    private final BarrioService barrioService;

    /**
     * Inyección de dependencias por constructor.
     *
     * Reutilizamos los services que ya tenemos creados
     * en lugar de consultar directamente los repositories.
     */
    public FiltroService(
            CategoriaDeportivaService categoriaDeportivaService,
            DeporteService deporteService,
            CiudadService ciudadService,
            BarrioService barrioService
    ) {
        this.categoriaDeportivaService = categoriaDeportivaService;
        this.deporteService = deporteService;
        this.ciudadService = ciudadService;
        this.barrioService = barrioService;
    }

    /**
     * Obtiene todas las opciones necesarias para filtros.
     *
     * Incluye:
     * - categorías deportivas
     * - deportes
     * - ciudades
     * - barrios
     * - niveles fijos
     * - modalidades fijas
     * - ordenamientos disponibles
     *
     * @return opciones disponibles para el buscador.
     */
    public FiltroOpcionesDTO obtenerOpcionesDeFiltros() {
        List<CategoriaDeportivaDTO> categorias =
                categoriaDeportivaService.obtenerCategoriasActivas();

        List<DeporteDTO> deportes =
                deporteService.obtenerDeportesActivos();

        List<CiudadDTO> ciudades =
                ciudadService.obtenerCiudadesActivas();

        List<BarrioDTO> barrios =
                barrioService.obtenerBarriosActivos();

        List<String> niveles = List.of(
                "PRINCIPIANTE",
                "INTERMEDIO",
                "AVANZADO",
                "TODOS"
        );

        List<String> modalidades = List.of(
                "PRESENCIAL",
                "ONLINE",
                "MIXTA"
        );

        List<String> ordenes = List.of(
                "recientes",
                "precio_asc",
                "precio_desc",
                "titulo_asc"
        );

        return new FiltroOpcionesDTO(
                categorias,
                deportes,
                ciudades,
                barrios,
                niveles,
                modalidades,
                ordenes
        );
    }
}
