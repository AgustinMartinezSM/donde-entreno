package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.entity.CategoriaDeportiva;
import com.dondeentreno.api.mapper.CategoriaDeportivaMapper;
import com.dondeentreno.api.repository.CategoriaDeportivaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de CategoriaDeportiva.
 *
 * Esta capa contiene la lógica de negocio relacionada
 * con las categorías deportivas.
 */
@Service
public class CategoriaDeportivaService {

    private final CategoriaDeportivaRepository categoriaDeportivaRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta que este service necesita un CategoriaDeportivaRepository
     * y se lo pasa automáticamente.
     */
    public CategoriaDeportivaService(CategoriaDeportivaRepository categoriaDeportivaRepository) {
        this.categoriaDeportivaRepository = categoriaDeportivaRepository;
    }

    /**
     * Obtiene todas las categorías deportivas activas,
     * ordenadas por el campo orden de menor a mayor.
     *
     * @return Lista de categorías deportivas en formato DTO.
     */
    public List<CategoriaDeportivaDTO> obtenerCategoriasActivas() {
        List<CategoriaDeportiva> categorias =
                categoriaDeportivaRepository.findByActivaTrueOrderByOrdenAsc();

        return categorias.stream()
                .map(CategoriaDeportivaMapper::toDTO)
                .toList();
    }
}