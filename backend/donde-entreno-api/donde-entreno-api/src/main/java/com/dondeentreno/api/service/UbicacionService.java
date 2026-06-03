package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.UbicacionDTO;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.mapper.UbicacionMapper;
import com.dondeentreno.api.repository.UbicacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de Ubicacion.
 *
 * Esta capa contiene la lógica relacionada con las ubicaciones
 * donde se realizan actividades deportivas.
 */
@Service
public class UbicacionService {

    private final UbicacionRepository ubicacionRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public UbicacionService(UbicacionRepository ubicacionRepository) {
        this.ubicacionRepository = ubicacionRepository;
    }

    /**
     * Obtiene todas las ubicaciones activas ordenadas por nombre.
     *
     * @return lista de ubicaciones activas en formato DTO.
     */
    public List<UbicacionDTO> obtenerUbicacionesActivas() {
        List<Ubicacion> ubicaciones = ubicacionRepository.findByActivaTrueOrderByNombreAsc();

        return ubicaciones.stream()
                .map(UbicacionMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene ubicaciones activas de un perfil publicador.
     *
     * @param perfilPublicadorId ID del perfil publicador.
     * @return lista de ubicaciones activas de ese perfil.
     */
    public List<UbicacionDTO> obtenerUbicacionesPorPerfilPublicador(Long perfilPublicadorId) {
        List<Ubicacion> ubicaciones =
                ubicacionRepository.findByActivaTrueAndPerfilPublicador_IdOrderByNombreAsc(perfilPublicadorId);

        return ubicaciones.stream()
                .map(UbicacionMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene ubicaciones activas de una ciudad.
     *
     * @param ciudadId ID de la ciudad.
     * @return lista de ubicaciones activas de esa ciudad.
     */
    public List<UbicacionDTO> obtenerUbicacionesPorCiudad(Long ciudadId) {
        List<Ubicacion> ubicaciones =
                ubicacionRepository.findByActivaTrueAndCiudad_IdOrderByNombreAsc(ciudadId);

        return ubicaciones.stream()
                .map(UbicacionMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene ubicaciones activas de un barrio.
     *
     * @param barrioId ID del barrio.
     * @return lista de ubicaciones activas de ese barrio.
     */
    public List<UbicacionDTO> obtenerUbicacionesPorBarrio(Long barrioId) {
        List<Ubicacion> ubicaciones =
                ubicacionRepository.findByActivaTrueAndBarrio_IdOrderByNombreAsc(barrioId);

        return ubicaciones.stream()
                .map(UbicacionMapper::toDTO)
                .toList();
    }
}
