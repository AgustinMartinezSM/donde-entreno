package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.mapper.DeporteMapper;
import com.dondeentreno.api.repository.DeporteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de Deporte.
 *
 * Esta capa contiene la lógica relacionada con los deportes.
 */
@Service
public class DeporteService {

    private final DeporteRepository deporteRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public DeporteService(DeporteRepository deporteRepository) {
        this.deporteRepository = deporteRepository;
    }

    /**
     * Obtiene todos los deportes activos ordenados por orden.
     *
     * @return lista de deportes activos en formato DTO.
     */
    public List<DeporteDTO> obtenerDeportesActivos() {
        List<Deporte> deportes = deporteRepository.findByActivoTrueOrderByOrdenAsc();

        return deportes.stream()
                .map(DeporteMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene deportes activos filtrados por slug de categoría deportiva.
     *
     * Ejemplo:
     * categoriaSlug = "deportes-de-combate"
     *
     * @param categoriaSlug slug de la categoría deportiva.
     * @return lista de deportes activos de esa categoría.
     */
    public List<DeporteDTO> obtenerDeportesActivosPorCategoriaSlug(String categoriaSlug) {
        List<Deporte> deportes =
                deporteRepository.findByActivoTrueAndCategoriaDeportiva_SlugOrderByOrdenAsc(categoriaSlug);

        return deportes.stream()
                .map(DeporteMapper::toDTO)
                .toList();
    }
}