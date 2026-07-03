package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
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
     * Obtiene todas las ciudades activas ordenadas por orden editorial y nombre.
     *
     * @return lista de ciudades activas en formato DTO.
     */
    public List<CiudadDTO> obtenerCiudadesActivas() {
        List<Ciudad> ciudades = ciudadRepository.findByActivaTrueOrderByOrdenAscNombreAsc();

        return ciudades.stream()
                .map(CiudadMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene una ciudad activa por slug.
     *
     * @param slug slug recibido desde la URL.
     * @return ciudad activa en formato DTO.
     */
    public CiudadDTO obtenerCiudadActivaPorSlug(String slug) {
        String slugNormalizado = normalizarSlug(slug);

        Ciudad ciudad = ciudadRepository.findBySlugAndActivaTrue(slugNormalizado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudad no encontrada."));

        return CiudadMapper.toDTO(ciudad);
    }

    private String normalizarSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }

        return slug.trim().toLowerCase();
    }
}
