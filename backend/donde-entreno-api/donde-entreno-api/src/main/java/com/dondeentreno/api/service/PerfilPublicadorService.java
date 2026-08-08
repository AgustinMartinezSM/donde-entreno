package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
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

    private static final String MENSAJE_PERFIL_NO_ENCONTRADO =
            "El perfil publicador solicitado no existe o no está disponible.";

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
     * Obtiene un perfil publicador activo por ID.
     *
     * Aplica el mismo criterio de visibilidad que el listado público
     * (activo = true) para que un perfil no pueda aparecer en la lista y
     * responder 404 en su detalle.
     *
     * @param id ID del perfil publicador.
     * @return el perfil en formato DTO.
     * @throws RecursoNoEncontradoException si no existe o no está activo.
     */
    public PerfilPublicadorDTO obtenerPerfilActivoPorId(Long id) {
        if (id == null) {
            throw new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO);
        }

        return perfilPublicadorRepository.findByIdAndActivoTrue(id)
                .map(PerfilPublicadorMapper::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO));
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