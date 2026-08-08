package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.PerfilPublicadorMapper;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Spring detecta automáticamente los repositories
     * y los entrega a este service.
     */
    public PerfilPublicadorService(
            PerfilPublicadorRepository perfilPublicadorRepository,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository
    ) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
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

        return mapearConSeguidores(perfiles);
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

        PerfilPublicadorDTO dto = perfilPublicadorRepository.findByIdAndActivoTrue(id)
                .map(PerfilPublicadorMapper::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO));

        dto.setCantidadSeguidores(
                seguimientoPublicadorRepository.countByPerfilPublicador_Id(id)
        );

        return dto;
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

        return mapearConSeguidores(perfiles);
    }

    /**
     * Mapea a DTO y completa la cantidad de seguidores con un solo
     * query agrupado, para que un listado de N perfiles no dispare N
     * conteos.
     *
     * Los perfiles sin seguidores no vuelven en el GROUP BY, así que se
     * completan con cero: el campo nunca queda nulo en un listado.
     */
    private List<PerfilPublicadorDTO> mapearConSeguidores(List<PerfilPublicador> perfiles) {
        List<PerfilPublicadorDTO> dtos = perfiles.stream()
                .map(PerfilPublicadorMapper::toDTO)
                .toList();

        List<Long> ids = dtos.stream()
                .map(PerfilPublicadorDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        if (ids.isEmpty()) {
            return dtos;
        }

        Map<Long, Long> seguidoresPorPerfil = new HashMap<>();

        for (SeguimientoPublicadorRepository.ConteoSeguidores conteo
                : seguimientoPublicadorRepository.contarSeguidoresPorPerfiles(ids)) {
            if (conteo.getPerfilPublicadorId() != null) {
                seguidoresPorPerfil.put(conteo.getPerfilPublicadorId(), conteo.getCantidad());
            }
        }

        for (PerfilPublicadorDTO dto : dtos) {
            dto.setCantidadSeguidores(seguidoresPorPerfil.getOrDefault(dto.getId(), 0L));
        }

        return dtos;
    }
}