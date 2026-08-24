package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.UbicacionDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.UbicacionMapper;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * El publicador ajusta el PUNTO de sus propias sedes (Fase 7).
 *
 * Sin moderación a propósito: la coordenada es un dato objetivo y
 * verificable —o el pin está en su puerta o no está—, y si lo carga
 * mal el único perjudicado es él. Pasarlo por el admin, como el resto
 * de los cambios de actividad, garantizaría que el campo quede vacío
 * para siempre: hoy 4 de 9 ubicaciones de producción no tienen
 * coordenadas justamente porque nadie las carga.
 */
@Service
public class PublicadorUbicacionService {

    private static final Logger log =
            LoggerFactory.getLogger(PublicadorUbicacionService.class);

    private final UbicacionRepository ubicacionRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ResolutorCoordenadas resolutorCoordenadas;

    public PublicadorUbicacionService(
            UbicacionRepository ubicacionRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ResolutorCoordenadas resolutorCoordenadas
    ) {
        this.ubicacionRepository = ubicacionRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.resolutorCoordenadas = resolutorCoordenadas;
    }

    /** Las sedes propias, para elegir cuál corregir. */
    @Transactional(readOnly = true)
    public List<UbicacionDTO> listarMisUbicaciones(Long userId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        return ubicacionRepository
                .findByActivaTrueAndPerfilPublicador_IdOrderByNombreAsc(perfil.getId())
                .stream()
                .map(UbicacionMapper::toDTO)
                .toList();
    }

    /**
     * Guarda el punto de una sede propia a partir de lo que el
     * publicador pegó (link de Google Maps o coordenadas).
     */
    @Transactional
    public UbicacionDTO guardarCoordenadas(Long userId, Long ubicacionId, String pegado) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        Ubicacion ubicacion = ubicacionRepository.findById(ubicacionId)
                .filter(encontrada -> encontrada.getPerfilPublicador() != null
                        && encontrada.getPerfilPublicador().getId().equals(perfil.getId())
                        && Boolean.TRUE.equals(encontrada.getActiva())
                        && encontrada.getDeletedAt() == null)
                /* 404 y no 403: no delatamos sedes ajenas. */
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la ubicacion."
                ));

        BigDecimal[] coordenadas = resolutorCoordenadas.resolver(pegado);

        ubicacion.setLatitud(coordenadas[0]);
        ubicacion.setLongitud(coordenadas[1]);
        ubicacion.setUpdatedAt(OffsetDateTime.now());

        Ubicacion guardada = ubicacionRepository.save(ubicacion);

        /* Solo metadata: la línea tiene que poder greparse. */
        log.info(
                "Publicador: UBICACION_COORDENADAS_CARGADAS ubicacionId={} perfilId={}",
                guardada.getId(),
                perfil.getId()
        );

        return UbicacionMapper.toDTO(guardada);
    }

    private PerfilPublicador obtenerPerfil(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Perfil publicador no encontrado."
                ));
    }
}
