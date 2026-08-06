package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActualizarPerfilPublicadorRequestDTO;
import com.dondeentreno.api.dto.PerfilPublicadorActualDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorRequestDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorResumenDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.mapper.SolicitudPublicadorMapper;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Service para operaciones del panel publicador autenticado.
 */
@Service
public class PublicadorService {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_EN_REVISION = "EN_REVISION";
    private static final String ESTADO_APROBADA = "APROBADA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final String MENSAJE_PERFIL_NO_ENCONTRADO = "Perfil publicador no encontrado.";

    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final SolicitudPublicacionRepository solicitudPublicacionRepository;
    private final SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudPublicacionService solicitudPublicacionService;

    public PublicadorService(
            PerfilPublicadorRepository perfilPublicadorRepository,
            SolicitudPublicacionRepository solicitudPublicacionRepository,
            SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository,
            UsuarioRepository usuarioRepository,
            SolicitudPublicacionService solicitudPublicacionService
    ) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
        this.solicitudPublicacionHorarioRepository = solicitudPublicacionHorarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudPublicacionService = solicitudPublicacionService;
    }

    @Transactional(readOnly = true)
    public PerfilPublicadorActualDTO obtenerMiPerfil(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        PerfilPublicador perfil = perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro un perfil publicador para el usuario autenticado."
                ));

        return PerfilPublicadorActualDTO.desdePerfil(perfil);
    }

    /**
     * Actualiza los campos de edicion directa del perfil del publicador
     * autenticado: descripcion, instagram y email de contacto.
     *
     * Semantica PATCH: un campo null no se toca; un campo vacio o con
     * solo espacios limpia el valor (queda null). Los campos sensibles
     * (nombre publico, tipo, ciudad, whatsapp/telefono, estado) se
     * editaran mas adelante mediante un flujo con revision admin.
     *
     * @param userId id del usuario autenticado (claim del JWT).
     * @param request campos a actualizar.
     * @return perfil actualizado.
     */
    @Transactional
    public PerfilPublicadorActualDTO actualizarMiPerfil(
            Long userId,
            ActualizarPerfilPublicadorRequestDTO request
    ) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        PerfilPublicador perfil = perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro un perfil publicador para el usuario autenticado."
                ));

        if (request.getDescripcion() != null) {
            perfil.setDescripcion(normalizarTextoEditable(request.getDescripcion()));
        }

        if (request.getInstagram() != null) {
            perfil.setInstagram(normalizarTextoEditable(request.getInstagram()));
        }

        if (request.getEmailContacto() != null) {
            perfil.setEmailContacto(normalizarTextoEditable(request.getEmailContacto()));
        }

        PerfilPublicador guardado = perfilPublicadorRepository.save(perfil);

        return PerfilPublicadorActualDTO.desdePerfil(guardado);
    }

    /**
     * Recorta espacios y convierte el vacio en null (limpiar campo).
     */
    private String normalizarTextoEditable(String valor) {
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<SolicitudPublicadorResumenDTO> listarMisSolicitudes(
            Long userId,
            String estado,
            int page,
            int size,
            String orden
    ) {
        ContextoPublicador contexto = obtenerContextoPublicador(userId);
        int paginaSegura = Math.max(page, 0);
        int tamanioSeguro = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(paginaSegura, tamanioSeguro, obtenerOrdenamiento(orden));
        String estadoNormalizado = normalizarEstadoListado(estado);

        Page<SolicitudPublicacion> paginaSolicitudes = estadoNormalizado == null
                ? solicitudPublicacionRepository.findByUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                        contexto.usuario().getId(),
                        contexto.perfil().getId(),
                        pageable
                )
                : solicitudPublicacionRepository.findByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
                        contexto.usuario().getId(),
                        contexto.perfil().getId(),
                        estadoNormalizado,
                        pageable
                );

        List<SolicitudPublicadorResumenDTO> contenido = paginaSolicitudes.getContent()
                .stream()
                .map(SolicitudPublicadorMapper::toResumenDTO)
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
    public SolicitudPublicadorDetalleDTO obtenerMiSolicitud(Long userId, Long solicitudId) {
        ContextoPublicador contexto = obtenerContextoPublicador(userId);
        SolicitudPublicacion solicitud = solicitudPublicacionRepository
                .findByIdAndUsuario_IdAndPerfilPublicador_IdAndDeletedAtIsNull(
                        solicitudId,
                        contexto.usuario().getId(),
                        contexto.perfil().getId()
                )
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada."));

        List<SolicitudPublicacionHorario> horarios = solicitudPublicacionHorarioRepository
                .findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(solicitud.getId());

        return SolicitudPublicadorMapper.toDetalleDTO(solicitud, horarios);
    }

    @Transactional
    public SolicitudPublicacionResponseDTO crearMiSolicitud(
            Long userId,
            SolicitudPublicadorRequestDTO request
    ) {
        ContextoPublicador contexto = obtenerContextoPublicador(userId);
        return solicitudPublicacionService.crearSolicitudPublicador(
                request,
                contexto.usuario(),
                contexto.perfil()
        );
    }

    private ContextoPublicador obtenerContextoPublicador(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Usuario usuario = usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO));

        PerfilPublicador perfil = perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO));

        return new ContextoPublicador(usuario, perfil);
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

    private boolean esEstadoListadoValido(String estado) {
        return ESTADO_PENDIENTE.equals(estado)
                || ESTADO_EN_REVISION.equals(estado)
                || ESTADO_APROBADA.equals(estado)
                || ESTADO_RECHAZADA.equals(estado);
    }

    /**
     * Define el criterio de ordenamiento del listado.
     *
     * Si viene un valor desconocido, lanzamos FiltroInvalidoException
     * para que la API responda 400 en vez de ordenar por "recientes"
     * en silencio.
     */
    private Sort obtenerOrdenamiento(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String ordenNormalizado = orden.trim().toLowerCase(Locale.ROOT);

        return switch (ordenNormalizado) {
            case "antiguos" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "recientes" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new FiltroInvalidoException(
                    "El parametro 'orden' tiene un valor invalido: '" + orden
                            + "'. Valores permitidos: antiguos, recientes."
            );
        };
    }

    private record ContextoPublicador(Usuario usuario, PerfilPublicador perfil) {
    }
}
