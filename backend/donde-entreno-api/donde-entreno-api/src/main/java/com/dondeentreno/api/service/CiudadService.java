package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.mapper.CiudadMapper;
import com.dondeentreno.api.repository.CiudadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de Ciudad.
 *
 * Esta capa contiene la lógica relacionada con las ciudades
 * disponibles en DondeEntreno.
 */
@Service
public class CiudadService {

    private final CiudadRepository ciudadRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public CiudadService(CiudadRepository ciudadRepository) {
        this.ciudadRepository = ciudadRepository;
    }

    /**
     * Obtiene todas las ciudades activas ordenadas por nombre.
     *
     * @return lista de ciudades activas en formato DTO.
     */
    public List<CiudadDTO> obtenerCiudadesActivas() {
        List<Ciudad> ciudades = ciudadRepository.findByActivaTrueOrderByNombreAsc();

        return ciudades.stream()
                .map(CiudadMapper::toDTO)
                .toList();
    }
}