package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionCambiarEstadoRequestDTO;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.mapper.SolicitudPublicacionAdminMapper;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Service admin para revisar solicitudes de publicacion.
 */
@Service
public class SolicitudPublicacionAdminService {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_EN_REVISION = "EN_REVISION";
    private static final String ESTADO_APROBADA = "APROBADA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";

    private final SolicitudPublicacionRepository solicitudPublicacionRepository;
    private final SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;
    private final UsuarioRepository usuarioRepository;

    public SolicitudPublicacionAdminService(
            SolicitudPublicacionRepository solicitudPublicacionRepository,
            SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
        this.solicitudPublicacionHorarioRepository = solicitudPublicacionHorarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<SolicitudPublicacionAdminResumenDTO> listarSolicitudes(
            String estado,
            int page,
            int size,
            String orden
    ) {
        int paginaSegura = Math.max(page, 0);
        int tamanioSeguro = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(paginaSegura, tamanioSeguro, obtenerOrdenamiento(orden));

        String estadoNormalizado = normalizarEstadoListado(estado);
        Page<SolicitudPublicacion> paginaSolicitudes = estadoNormalizado == null
                ? solicitudPublicacionRepository.findByDeletedAtIsNull(pageable)
                : solicitudPublicacionRepository.findByEstadoAndDeletedAtIsNull(estadoNormalizado, pageable);

        List<SolicitudPublicacionAdminResumenDTO> contenido = paginaSolicitudes.getContent()
                .stream()
                .map(SolicitudPublicacionAdminMapper::toResumenDTO)
                .toList();

        return new PaginaResponseDTO<>(
                contenido,
                paginaSolicitudes.getNumber(),
                paginaSolicitudes.getSize(),
                paginaSolicitudes.getTotalElements(),
                paginaSolicitudes.getTotalPages(),
                paginaSolicitudes.isLast()
        );
    }

    @Transactional(readOnly = true)
    public SolicitudPublicacionAdminDetalleDTO obtenerDetalle(Long id) {
        SolicitudPublicacion solicitud = buscarSolicitudActiva(id);
        List<SolicitudPublicacionHorario> horarios = buscarHorariosOrdenados(solicitud.getId());

        return SolicitudPublicacionAdminMapper.toDetalleDTO(solicitud, horarios);
    }

    @Transactional
    public SolicitudPublicacionAdminDetalleDTO cambiarEstado(
            Long id,
            SolicitudPublicacionCambiarEstadoRequestDTO request,
            Long usuarioAutenticadoId
    ) {
        SolicitudPublicacion solicitud = buscarSolicitudActiva(id);
        Usuario usuarioRevisor = buscarUsuarioRevisor(usuarioAutenticadoId);
        String estadoNormalizado = normalizarEstadoPatch(request == null ? null : request.getEstado());
        OffsetDateTime ahora = OffsetDateTime.now();

        solicitud.setRevisadoPorUsuario(usuarioRevisor);
        solicitud.setUpdatedAt(ahora);

        if (ESTADO_EN_REVISION.equals(estadoNormalizado)) {
            pasarAEnRevision(solicitud, ahora);
        } else if (ESTADO_RECHAZADA.equals(estadoNormalizado)) {
            pasarARechazada(solicitud, request.getMotivoRechazo(), ahora);
        } else {
            throw new SolicitudPublicacionInvalidaException("Estado de solicitud invalido.");
        }

        SolicitudPublicacion solicitudGuardada = solicitudPublicacionRepository.save(solicitud);
        List<SolicitudPublicacionHorario> horarios = buscarHorariosOrdenados(solicitudGuardada.getId());

        return SolicitudPublicacionAdminMapper.toDetalleDTO(solicitudGuardada, horarios);
    }

    private SolicitudPublicacion buscarSolicitudActiva(Long id) {
        return solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de publicacion no encontrada."));
    }

    private List<SolicitudPublicacionHorario> buscarHorariosOrdenados(Long solicitudId) {
        return solicitudPublicacionHorarioRepository
                .findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(solicitudId);
    }

    private Usuario buscarUsuarioRevisor(Long usuarioAutenticadoId) {
        if (usuarioAutenticadoId == null) {
            throw new SolicitudPublicacionInvalidaException("Usuario autenticado invalido.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioAutenticadoId)
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException("Usuario autenticado invalido."));

        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getDeletedAt() != null) {
            throw new SolicitudPublicacionInvalidaException("Usuario autenticado invalido.");
        }

        return usuario;
    }

    private void pasarAEnRevision(SolicitudPublicacion solicitud, OffsetDateTime ahora) {
        solicitud.setEstado(ESTADO_EN_REVISION);
        solicitud.setMotivoRechazo(null);

        if (solicitud.getRevisionIniciadaAt() == null) {
            solicitud.setRevisionIniciadaAt(ahora);
        }
    }

    private void pasarARechazada(SolicitudPublicacion solicitud, String motivoRechazo, OffsetDateTime ahora) {
        String motivoNormalizado = normalizarMotivoRechazo(motivoRechazo);
        if (motivoNormalizado == null) {
            throw new SolicitudPublicacionInvalidaException("El motivo de rechazo es obligatorio.");
        }

        solicitud.setEstado(ESTADO_RECHAZADA);
        solicitud.setMotivoRechazo(motivoNormalizado);

        if (solicitud.getRevisionIniciadaAt() == null) {
            solicitud.setRevisionIniciadaAt(ahora);
        }

        solicitud.setRevisionFinalizadaAt(ahora);
    }

    private String normalizarEstadoListado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }

        String estadoNormalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (esEstadoListadoValido(estadoNormalizado)) {
            return estadoNormalizado;
        }

        throw new SolicitudPublicacionInvalidaException("Estado de solicitud invalido.");
    }

    private String normalizarEstadoPatch(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new SolicitudPublicacionInvalidaException("Estado de solicitud invalido.");
        }

        return estado.trim().toUpperCase(Locale.ROOT);
    }

    private boolean esEstadoListadoValido(String estado) {
        return ESTADO_PENDIENTE.equals(estado)
                || ESTADO_EN_REVISION.equals(estado)
                || ESTADO_APROBADA.equals(estado)
                || ESTADO_RECHAZADA.equals(estado);
    }

    private String normalizarMotivoRechazo(String motivoRechazo) {
        if (motivoRechazo == null || motivoRechazo.isBlank()) {
            return null;
        }

        return motivoRechazo.trim();
    }

    private Sort obtenerOrdenamiento(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String ordenNormalizado = orden.trim().toLowerCase(Locale.ROOT);

        return switch (ordenNormalizado) {
            case "antiguos" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "recientes" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
