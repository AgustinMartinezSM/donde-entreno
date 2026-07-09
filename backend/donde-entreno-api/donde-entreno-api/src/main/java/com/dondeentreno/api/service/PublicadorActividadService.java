package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.PublicadorActividadDetalleDTO;
import com.dondeentreno.api.dto.PublicadorActividadResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.PublicadorActividadMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Service para actividades reales del publicador autenticado.
 */
@Service
public class PublicadorActividadService {

    private static final String ESTADO_PUBLICADA = "PUBLICADA";
    private static final String MENSAJE_PERFIL_NO_ENCONTRADO = "Perfil publicador no encontrado.";
    private static final String MENSAJE_ACTIVIDAD_NO_ENCONTRADA = "Actividad no encontrada.";

    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final HorarioActividadRepository horarioActividadRepository;
    private final ImagenRepository imagenRepository;
    private final SolicitudPublicacionRepository solicitudPublicacionRepository;

    public PublicadorActividadService(
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            HorarioActividadRepository horarioActividadRepository,
            ImagenRepository imagenRepository,
            SolicitudPublicacionRepository solicitudPublicacionRepository
    ) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.horarioActividadRepository = horarioActividadRepository;
        this.imagenRepository = imagenRepository;
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<PublicadorActividadResumenDTO> listarMisActividades(
            Long userId,
            int page,
            int size,
            String orden
    ) {
        PerfilPublicador perfil = obtenerPerfilPublicador(userId);
        Pageable pageable = PageRequest.of(
                validarPagina(page),
                validarTamanioPagina(size),
                obtenerOrdenamiento(orden)
        );

        Page<Actividad> paginaActividades =
                actividadRepository.findByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        perfil.getId(),
                        ESTADO_PUBLICADA,
                        pageable
                );

        List<PublicadorActividadResumenDTO> contenido = paginaActividades.getContent()
                .stream()
                .map(this::toResumenDTO)
                .toList();

        return new PaginaResponseDTO<>(
                contenido,
                paginaActividades.getNumber(),
                paginaActividades.getSize(),
                paginaActividades.getTotalElements(),
                paginaActividades.getTotalPages(),
                paginaActividades.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PublicadorActividadDetalleDTO obtenerMiActividad(Long userId, Long actividadId) {
        PerfilPublicador perfil = obtenerPerfilPublicador(userId);
        Actividad actividad = actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        actividadId,
                        perfil.getId(),
                        ESTADO_PUBLICADA
                )
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_ACTIVIDAD_NO_ENCONTRADA));

        List<HorarioActividad> horarios = horarioActividadRepository
                .findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(actividad.getId());
        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndActividad_IdOrderByOrdenAsc(actividad.getId());
        SolicitudPublicacion solicitudOrigen = solicitudPublicacionRepository
                .findByActividadGenerada_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                        actividad.getId(),
                        perfil.getId()
                )
                .orElse(null);

        return PublicadorActividadMapper.toDetalleDTO(
                actividad,
                horarios,
                imagenes,
                solicitudOrigen
        );
    }

    private PublicadorActividadResumenDTO toResumenDTO(Actividad actividad) {
        List<Imagen> imagenes = imagenRepository
                .findByActivaTrueAndActividad_IdOrderByOrdenAsc(actividad.getId());
        String imagenPrincipalUrl = PublicadorActividadMapper.resolverImagenPrincipalUrl(imagenes);
        return PublicadorActividadMapper.toResumenDTO(actividad, imagenPrincipalUrl);
    }

    private PerfilPublicador obtenerPerfilPublicador(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO));
    }

    private int validarPagina(int page) {
        return Math.max(page, 0);
    }

    private int validarTamanioPagina(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private Sort obtenerOrdenamiento(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String ordenNormalizado = orden.trim().toLowerCase(Locale.ROOT);
        return switch (ordenNormalizado) {
            case "antiguos" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "titulo_asc" -> Sort.by(Sort.Direction.ASC, "titulo");
            case "recientes" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
