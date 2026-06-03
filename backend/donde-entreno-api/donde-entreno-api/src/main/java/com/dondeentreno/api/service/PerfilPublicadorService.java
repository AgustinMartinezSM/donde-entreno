package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.mapper.PerfilPublicadorMapper;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de PerfilPublicador.
 *
 * Esta capa contiene la lógica relacionada con los perfiles
 * que publican actividades en DondeEntreno.
 */
@Service
public class PerfilPublicadorService {

    private final PerfilPublicadorRepository perfilPublicadorRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente el repository
     * y lo entrega a este service.
     */
    public PerfilPublicadorService(PerfilPublicadorRepository perfilPublicadorRepository) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
    }

    /**
     * Obtiene todos los perfiles publicadores activos,
     * ordenados por nombre.
     *
     * @return lista de perfiles publicadores activos en formato DTO.
     */
    public List<PerfilPublicadorDTO> obtenerPerfilesActivos() {
        List<PerfilPublicador> perfiles =
                perfilPublicadorRepository.findByActivoTrueOrderByNombreAsc();

        return perfiles.stream()
                .map(PerfilPublicadorMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene perfiles activos filtrados por tipo.
     *
     * @param tipoPublicador tipo de publicador.
     * @return lista de perfiles activos de ese tipo.
     */
    public List<PerfilPublicadorDTO> obtenerPerfilesActivosPorTipo(String tipoPublicador) {
        List<PerfilPublicador> perfiles =
                perfilPublicadorRepository.findByActivoTrueAndTipoPublicadorOrderByNombreAsc(tipoPublicador);

        return perfiles.stream()
                .map(PerfilPublicadorMapper::toDTO)
                .toList();
    }
}