package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.EstadoSeguimientoDTO;
import com.dondeentreno.api.dto.SeguimientoPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.ActividadMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Seguir / dejar de seguir publicadores (capa social, Bloque 8).
 *
 * Todo se acota al usuario autenticado (resuelto desde el userId del
 * JWT). Seguir es idempotente (respaldado por el UNIQUE de la tabla).
 */
@Service
public class SeguimientoPublicadorService {

    private static final String MENSAJE_PUBLICADOR_NO_ENCONTRADO = "Publicador no encontrado.";
    private static final String MENSAJE_NO_AUTENTICADO = "No autenticado.";
    private static final String ESTADO_ACTIVIDAD_PUBLICADA = "PUBLICADA";

    /**
     * Tope del feed de novedades: las últimas N actividades publicadas
     * entre todos los publicadores seguidos. V1 sin paginación.
     */
    private static final int TAMANIO_FEED = 20;

    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActividadRepository actividadRepository;
    private final ImagenService imagenService;

    public SeguimientoPublicadorService(
            SeguimientoPublicadorRepository seguimientoPublicadorRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            UsuarioRepository usuarioRepository,
            ActividadRepository actividadRepository,
            ImagenService imagenService
    ) {
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.actividadRepository = actividadRepository;
        this.imagenService = imagenService;
    }

    @Transactional
    public EstadoSeguimientoDTO seguir(Long userId, Long perfilPublicadorId) {
        validarUserId(userId);

        PerfilPublicador perfil = perfilPublicadorRepository
                .findByIdAndActivoTrue(perfilPublicadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PUBLICADOR_NO_ENCONTRADO));

        if (!seguimientoPublicadorRepository
                .existsByUsuario_IdAndPerfilPublicador_Id(userId, perfilPublicadorId)) {
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_NO_AUTENTICADO));

            SeguimientoPublicador seguimiento = new SeguimientoPublicador();
            seguimiento.setUsuario(usuario);
            seguimiento.setPerfilPublicador(perfil);
            seguimiento.setCreatedAt(OffsetDateTime.now());
            seguimientoPublicadorRepository.save(seguimiento);
        }

        return new EstadoSeguimientoDTO(true);
    }

    @Transactional
    public void dejarDeSeguir(Long userId, Long perfilPublicadorId) {
        validarUserId(userId);
        seguimientoPublicadorRepository
                .deleteByUsuario_IdAndPerfilPublicador_Id(userId, perfilPublicadorId);
    }

    @Transactional(readOnly = true)
    public EstadoSeguimientoDTO estado(Long userId, Long perfilPublicadorId) {
        validarUserId(userId);
        return new EstadoSeguimientoDTO(
                seguimientoPublicadorRepository
                        .existsByUsuario_IdAndPerfilPublicador_Id(userId, perfilPublicadorId)
        );
    }

    /**
     * Feed de novedades: últimas actividades publicadas de los
     * publicadores que sigue el usuario, más recientes primero.
     * Sin seguidos devuelve lista vacía (no consulta actividades).
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> obtenerFeedActividades(Long userId) {
        validarUserId(userId);

        List<Long> perfilesSeguidos = seguimientoPublicadorRepository
                .findByUsuario_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(seguimiento -> seguimiento.getPerfilPublicador().getId())
                .toList();

        if (perfilesSeguidos.isEmpty()) {
            return List.of();
        }

        List<ActividadDTO> feed = actividadRepository
                .findByActivaTrueAndEstadoPublicacionAndDeletedAtIsNullAndPerfilPublicador_IdInOrderByCreatedAtDesc(
                        ESTADO_ACTIVIDAD_PUBLICADA,
                        perfilesSeguidos,
                        PageRequest.of(0, TAMANIO_FEED)
                )
                .stream()
                .map(ActividadMapper::toDTO)
                .toList();

        /* El feed usa las mismas cards públicas: también lleva imagen. */
        imagenService.asignarImagenPrincipal(feed);

        return feed;
    }

    @Transactional(readOnly = true)
    public List<SeguimientoPublicadorDTO> listarSeguidos(Long userId) {
        validarUserId(userId);
        List<SeguimientoPublicadorDTO> seguidos = seguimientoPublicadorRepository
                .findByUsuario_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();

        asignarLogos(seguidos);

        return seguidos;
    }

    /** Identidad única (fix UX 2026-08-22): el logo sale de ImagenService. */
    private void asignarLogos(List<SeguimientoPublicadorDTO> seguidos) {
        List<Long> perfilIds = seguidos.stream()
                .map(SeguimientoPublicadorDTO::getPerfilPublicadorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        java.util.Map<Long, String> logoPorPerfil =
                imagenService.obtenerLogosAprobadosPorPerfil(perfilIds);

        for (SeguimientoPublicadorDTO seguido : seguidos) {
            seguido.setPerfilLogoUrl(logoPorPerfil.get(seguido.getPerfilPublicadorId()));
        }
    }

    private SeguimientoPublicadorDTO toDTO(SeguimientoPublicador seguimiento) {
        PerfilPublicador perfil = seguimiento.getPerfilPublicador();
        String ciudad = perfil.getCiudadPrincipal() != null
                ? perfil.getCiudadPrincipal().getNombre()
                : null;

        return new SeguimientoPublicadorDTO(
                perfil.getId(),
                perfil.getNombre(),
                perfil.getTipoPublicador(),
                ciudad,
                seguimiento.getCreatedAt()
        );
    }

    private void validarUserId(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException(MENSAJE_NO_AUTENTICADO);
        }
    }
}
