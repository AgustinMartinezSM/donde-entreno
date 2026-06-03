package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.mapper.BarrioMapper;
import com.dondeentreno.api.repository.BarrioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de Barrio.
 *
 * Esta capa contiene la lógica relacionada con los barrios
 * o zonas disponibles en DondeEntreno.
 */
@Service
public class BarrioService {

    private final BarrioRepository barrioRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public BarrioService(BarrioRepository barrioRepository) {
        this.barrioRepository = barrioRepository;
    }

    /**
     * Obtiene todos los barrios activos ordenados por nombre.
     *
     * @return lista de barrios activos en formato DTO.
     */
    public List<BarrioDTO> obtenerBarriosActivos() {
        List<Barrio> barrios = barrioRepository.findByActivoTrueOrderByNombreAsc();

        return barrios.stream()
                .map(BarrioMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene barrios activos filtrados por ciudad.
     *
     * @param ciudadId ID de la ciudad.
     * @return lista de barrios activos de esa ciudad.
     */
    public List<BarrioDTO> obtenerBarriosActivosPorCiudad(Long ciudadId) {
        List<Barrio> barrios = barrioRepository.findByActivoTrueAndCiudad_IdOrderByNombreAsc(ciudadId);

        return barrios.stream()
                .map(BarrioMapper::toDTO)
                .toList();
    }
}