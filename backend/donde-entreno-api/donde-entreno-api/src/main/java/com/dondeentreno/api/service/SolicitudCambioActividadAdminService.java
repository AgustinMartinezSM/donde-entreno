package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActualizarEstadoSolicitudCambioRequestDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.dto.SolicitudCambioResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.SolicitudCambioActividad;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudCambioInvalidaException;
import com.dondeentreno.api.mapper.SolicitudCambioActividadMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.dondeentreno.api.service.SolicitudCambioActividadService.ESTADO_APROBADA;
import static com.dondeentreno.api.service.SolicitudCambioActividadService.ESTADO_EN_REVISION;
import static com.dondeentreno.api.service.SolicitudCambioActividadService.ESTADO_PENDIENTE;
import static com.dondeentreno.api.service.SolicitudCambioActividadService.ESTADO_RECHAZADA;

/**
 * Flujo administrativo de solicitudes de cambio: cola de revision,
 * detalle con comparacion antes/despues, cambio de estado y
 * aprobacion transaccional (recien aca cambia la actividad publica).
 */
@Service
public class SolicitudCambioActividadAdminService {

    private static final String MOTIVO_RECHAZO_AUTOMATICO =
            "La actividad ya no esta disponible para aplicar cambios "
                    + "(fue despublicada, eliminada o cambio de duenio).";

    private final SolicitudCambioActividadRepository solicitudCambioRepository;
    private final ActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;

    public SolicitudCambioActividadAdminService(
            SolicitudCambioActividadRepository solicitudCambioRepository,
            ActividadRepository actividadRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.solicitudCambioRepository = solicitudCambioRepository;
        this.actividadRepository = actividadRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<SolicitudCambioResumenDTO> listar(
            String estado,
            int page,
            int size,
            String orden
    ) {
        Pageable pageable = SolicitudCambioActividadService.construirPageable(page, size, orden);
        String estadoFiltro = SolicitudCambioActividadService.validarEstadoFiltro(estado);

        Page<SolicitudCambioActividad> pagina = estadoFiltro != null
                ? solicitudCambioRepository.findByEstadoAndDeletedAtIsNull(estadoFiltro, pageable)
                : solicitudCambioRepository.findByDeletedAtIsNull(pageable);

        return SolicitudCambioActividadService.mapearPagina(pagina);
    }

    @Transactional(readOnly = true)
    public SolicitudCambioDetalleDTO obtenerDetalle(Long solicitudId) {
        SolicitudCambioActividad solicitud = buscarSolicitudActiva(solicitudId);

        return SolicitudCambioDetalleDTO.desdeEntidad(
                solicitud,
                SolicitudCambioActividadMapper.construirCambios(
                        solicitud,
                        solicitud.getActividad()
                )
        );
    }

    /**
     * Cambia el estado administrativo: EN_REVISION o RECHAZADA
     * (con motivo obligatorio). La aprobacion va por aprobar().
     */
    @Transactional
    public SolicitudCambioDetalleDTO actualizarEstado(
            Long solicitudId,
            ActualizarEstadoSolicitudCambioRequestDTO request,
            Long adminUserId
    ) {
        SolicitudCambioActividad solicitud = buscarSolicitudActiva(solicitudId);
        validarEstadoAbierto(solicitud);

        String estadoNuevo = request.getEstado() != null
                ? request.getEstado().trim().toUpperCase(Locale.ROOT)
                : "";

        OffsetDateTime ahora = OffsetDateTime.now();

        if (ESTADO_EN_REVISION.equals(estadoNuevo)) {
            solicitud.setEstado(ESTADO_EN_REVISION);
            solicitud.setUpdatedAt(ahora);
        } else if (ESTADO_RECHAZADA.equals(estadoNuevo)) {
            String motivo = request.getMotivoRechazo() != null
                    ? request.getMotivoRechazo().trim()
                    : "";

            if (motivo.isEmpty()) {
                throw new SolicitudCambioInvalidaException(
                        "Para rechazar una solicitud de cambio hay que indicar el motivo."
                );
            }

            Usuario admin = buscarUsuarioAdmin(adminUserId);
            solicitud.setEstado(ESTADO_RECHAZADA);
            solicitud.setMotivoRechazo(motivo);
            solicitud.setResueltoPor(admin);
            solicitud.setResueltoAt(ahora);
            solicitud.setUpdatedAt(ahora);
        } else {
            throw new SolicitudCambioInvalidaException(
                    "Estado invalido. Valores permitidos: EN_REVISION, RECHAZADA."
            );
        }

        SolicitudCambioActividad guardada = solicitudCambioRepository.save(solicitud);

        return SolicitudCambioDetalleDTO.desdeEntidad(
                guardada,
                SolicitudCambioActividadMapper.construirCambios(
                        guardada,
                        guardada.getActividad()
                )
        );
    }

    /**
     * Aprueba la solicitud y aplica los cambios sobre la actividad,
     * todo en una transaccion.
     *
     * Antes de aplicar revalida que la actividad siga publicada,
     * activa y del mismo perfil: si ya no lo esta, la solicitud se
     * rechaza automaticamente con un motivo claro en lugar de aplicar
     * cambios sobre una actividad invalida.
     */
    @Transactional
    public SolicitudCambioDetalleDTO aprobar(Long solicitudId, Long adminUserId) {
        SolicitudCambioActividad solicitud = buscarSolicitudActiva(solicitudId);
        validarEstadoAbierto(solicitud);

        Usuario admin = buscarUsuarioAdmin(adminUserId);
        OffsetDateTime ahora = OffsetDateTime.now();

        /*
          Publicada O pausada (fase 6): un cambio aprobado sobre una
          actividad pausada aplica igual y queda listo para cuando el
          publicador la reanude. El rechazo automatico queda solo para
          actividades realmente dadas de baja.
        */
        Optional<Actividad> actividadVigente = actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        solicitud.getActividad().getId(),
                        solicitud.getPerfilPublicador().getId(),
                        PublicadorActividadService.ESTADOS_DEL_PANEL
                );

        if (actividadVigente.isEmpty()) {
            solicitud.setEstado(ESTADO_RECHAZADA);
            solicitud.setMotivoRechazo(MOTIVO_RECHAZO_AUTOMATICO);
            solicitud.setResueltoPor(admin);
            solicitud.setResueltoAt(ahora);
            solicitud.setUpdatedAt(ahora);

            SolicitudCambioActividad rechazada = solicitudCambioRepository.save(solicitud);

            return SolicitudCambioDetalleDTO.desdeEntidad(
                    rechazada,
                    SolicitudCambioActividadMapper.construirCambios(
                            rechazada,
                            rechazada.getActividad()
                    )
            );
        }

        Actividad actividad = actividadVigente.get();

        /*
          La comparacion se arma ANTES de aplicar, para que el DTO
          devuelto muestre el antes/despues real de esta aprobacion.
        */
        List<com.dondeentreno.api.dto.CampoCambioDTO> cambios =
                SolicitudCambioActividadMapper.construirCambios(solicitud, actividad);

        SolicitudCambioActividadMapper.aplicarCambios(solicitud, actividad);
        actividad.setUpdatedAt(ahora);
        actividadRepository.save(actividad);

        solicitud.setEstado(ESTADO_APROBADA);
        solicitud.setResueltoPor(admin);
        solicitud.setResueltoAt(ahora);
        solicitud.setUpdatedAt(ahora);

        SolicitudCambioActividad guardada = solicitudCambioRepository.save(solicitud);

        return SolicitudCambioDetalleDTO.desdeEntidad(guardada, cambios);
    }

    private SolicitudCambioActividad buscarSolicitudActiva(Long solicitudId) {
        if (solicitudId == null) {
            throw new RecursoNoEncontradoException("No se encontro la solicitud de cambio.");
        }

        return solicitudCambioRepository
                .findByIdAndDeletedAtIsNull(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la solicitud de cambio."
                ));
    }

    private void validarEstadoAbierto(SolicitudCambioActividad solicitud) {
        String estado = solicitud.getEstado();

        if (ESTADO_APROBADA.equals(estado)) {
            throw new SolicitudCambioInvalidaException(
                    "La solicitud de cambio ya fue aprobada."
            );
        }

        if (ESTADO_RECHAZADA.equals(estado)) {
            throw new SolicitudCambioInvalidaException(
                    "La solicitud de cambio ya fue rechazada."
            );
        }

        if (!ESTADO_PENDIENTE.equals(estado) && !ESTADO_EN_REVISION.equals(estado)) {
            throw new SolicitudCambioInvalidaException(
                    "La solicitud de cambio no puede modificarse en su estado actual."
            );
        }
    }

    private Usuario buscarUsuarioAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return usuarioRepository.findById(adminUserId)
                .filter((usuario) -> Boolean.TRUE.equals(usuario.getActivo())
                        && usuario.getDeletedAt() == null)
                .orElseThrow(() -> new CredencialesInvalidasException("No autenticado."));
    }
}
