package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudCambioActividadRequestDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.dto.SolicitudCambioResumenDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudCambioActividad;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudCambioConflictoException;
import com.dondeentreno.api.exception.SolicitudCambioInvalidaException;
import com.dondeentreno.api.entity.SolicitudCambioHorario;
import com.dondeentreno.api.mapper.SolicitudCambioActividadMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
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
 * Flujo del publicador para solicitudes de cambio sobre actividades
 * publicadas: crear, listar las propias y ver el detalle.
 *
 * La actividad publica NO cambia aca: los cambios recien se aplican
 * cuando un admin aprueba (SolicitudCambioActividadAdminService).
 */
@Service
public class SolicitudCambioActividadService {

    static final String ESTADO_PENDIENTE = "PENDIENTE";
    static final String ESTADO_EN_REVISION = "EN_REVISION";
    static final String ESTADO_APROBADA = "APROBADA";
    static final String ESTADO_RECHAZADA = "RECHAZADA";
    static final String ESTADO_PUBLICACION_PUBLICADA = "PUBLICADA";

    static final List<String> ESTADOS_ABIERTOS =
            List.of(ESTADO_PENDIENTE, ESTADO_EN_REVISION);

    private static final List<String> ESTADOS_PERMITIDOS =
            List.of(ESTADO_PENDIENTE, ESTADO_EN_REVISION, ESTADO_APROBADA, ESTADO_RECHAZADA);

    private static final List<String> NIVELES_PERMITIDOS =
            List.of("PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "TODOS");

    private static final List<String> MODALIDADES_PERMITIDAS =
            List.of("PRESENCIAL", "ONLINE", "MIXTA");

    private static final List<String> ENFOQUES_PERMITIDOS =
            List.of("RECREATIVO", "COMPETITIVO", "MIXTO");

    private final SolicitudCambioActividadRepository solicitudCambioRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final DeporteRepository deporteRepository;
    private final BarrioRepository barrioRepository;

    public SolicitudCambioActividadService(
            SolicitudCambioActividadRepository solicitudCambioRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            DeporteRepository deporteRepository,
            BarrioRepository barrioRepository
    ) {
        this.solicitudCambioRepository = solicitudCambioRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.deporteRepository = deporteRepository;
        this.barrioRepository = barrioRepository;
    }

    /**
     * Crea una solicitud de cambio sobre una actividad publicada propia.
     *
     * Reglas:
     * - la actividad debe pertenecer al perfil del usuario y estar PUBLICADA;
     * - al menos un campo propuesto (los textos en blanco se descartan);
     * - nivel/modalidad dentro de los dominios permitidos;
     * - una sola solicitud abierta por actividad (si no, 409).
     */
    @Transactional
    public SolicitudCambioDetalleDTO crearSolicitud(
            Long userId,
            Long actividadId,
            SolicitudCambioActividadRequestDTO request
    ) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Actividad actividad = buscarActividadPropiaPublicada(actividadId, perfil.getId());

        SolicitudCambioActividad solicitud = construirSolicitudNormalizada(request, actividad);

        if (SolicitudCambioActividadMapper.listarCamposPropuestos(solicitud).isEmpty()) {
            throw new SolicitudCambioInvalidaException(
                    "La solicitud no propone ningun cambio."
            );
        }

        if (solicitudCambioRepository.existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
                actividad.getId(),
                ESTADOS_ABIERTOS
        )) {
            throw new SolicitudCambioConflictoException(
                    "La actividad ya tiene una solicitud de cambio abierta. "
                            + "Esperá a que se resuelva antes de pedir otra."
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        solicitud.setActividad(actividad);
        solicitud.setPerfilPublicador(perfil);
        solicitud.setUsuario(perfil.getUsuario());
        solicitud.setEstado(ESTADO_PENDIENTE);
        solicitud.setCreatedAt(ahora);
        solicitud.setUpdatedAt(ahora);

        SolicitudCambioActividad guardada = solicitudCambioRepository.save(solicitud);

        return SolicitudCambioDetalleDTO.desdeEntidad(
                guardada,
                SolicitudCambioActividadMapper.construirCambios(guardada, actividad)
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<SolicitudCambioResumenDTO> listarMias(
            Long userId,
            String estado,
            int page,
            int size,
            String orden
    ) {
        PerfilPublicador perfil = buscarPerfil(userId);
        Pageable pageable = construirPageable(page, size, orden);
        String estadoFiltro = validarEstadoFiltro(estado);

        Page<SolicitudCambioActividad> pagina = estadoFiltro != null
                ? solicitudCambioRepository.findByPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
                        perfil.getId(), estadoFiltro, pageable)
                : solicitudCambioRepository.findByPerfilPublicador_IdAndDeletedAtIsNull(
                        perfil.getId(), pageable);

        return mapearPagina(pagina);
    }

    @Transactional(readOnly = true)
    public SolicitudCambioDetalleDTO obtenerMia(Long userId, Long solicitudId) {
        PerfilPublicador perfil = buscarPerfil(userId);

        SolicitudCambioActividad solicitud = solicitudCambioRepository
                .findByIdAndPerfilPublicador_IdAndDeletedAtIsNull(solicitudId, perfil.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la solicitud de cambio."
                ));

        return SolicitudCambioDetalleDTO.desdeEntidad(
                solicitud,
                SolicitudCambioActividadMapper.construirCambios(solicitud, solicitud.getActividad())
        );
    }

    // ==========================================================
    // Helpers compartidos con el flujo admin
    // ==========================================================

    static PaginaResponseDTO<SolicitudCambioResumenDTO> mapearPagina(
            Page<SolicitudCambioActividad> pagina
    ) {
        List<SolicitudCambioResumenDTO> contenido = pagina.getContent().stream()
                .map((solicitud) -> SolicitudCambioResumenDTO.desdeEntidad(
                        solicitud,
                        SolicitudCambioActividadMapper.listarCamposPropuestos(solicitud)
                ))
                .toList();

        return new PaginaResponseDTO<>(
                contenido,
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isLast()
        );
    }

    static Pageable construirPageable(int page, int size, String orden) {
        int paginaSegura = Math.max(page, 0);
        int tamanioSeguro = Math.min(Math.max(size, 1), 50);

        return PageRequest.of(paginaSegura, tamanioSeguro, construirOrdenamiento(orden));
    }

    static String validarEstadoFiltro(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }

        String estadoNormalizado = estado.trim().toUpperCase(Locale.ROOT);

        if (!ESTADOS_PERMITIDOS.contains(estadoNormalizado)) {
            throw new FiltroInvalidoException(
                    "El parametro 'estado' tiene un valor invalido: '" + estado
                            + "'. Valores permitidos: "
                            + String.join(", ", ESTADOS_PERMITIDOS) + "."
            );
        }

        return estadoNormalizado;
    }

    private static Sort construirOrdenamiento(String orden) {
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

    private PerfilPublicador buscarPerfil(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro un perfil publicador para el usuario autenticado."
                ));
    }

    private Actividad buscarActividadPropiaPublicada(Long actividadId, Long perfilId) {
        if (actividadId == null) {
            throw new RecursoNoEncontradoException("No se encontro la actividad.");
        }

        /*
          Publicada O pausada (fase 6): en pausa tambien se pueden pedir
          cambios — pausar oculta al publico, no congela la gestion.
        */
        return actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionInAndDeletedAtIsNull(
                        actividadId,
                        perfilId,
                        PublicadorActividadService.ESTADOS_DEL_PANEL
                )
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro una actividad publicada de tu perfil con ese id."
                ));
    }

    /**
     * Normaliza el request: recorta textos (blanco = sin cambio) y
     * valida dominios, deporte, edades (contra el resultado combinado),
     * ubicacion (barrio de la MISMA ciudad de la actividad) y horarios
     * (reemplazo total con flag explicito, >=1 fila).
     */
    private SolicitudCambioActividad construirSolicitudNormalizada(
            SolicitudCambioActividadRequestDTO request,
            Actividad actividad
    ) {
        SolicitudCambioActividad solicitud = new SolicitudCambioActividad();

        solicitud.setTitulo(limpiarTexto(request.getTitulo()));
        solicitud.setDescripcion(limpiarTexto(request.getDescripcion()));
        solicitud.setPrecioReferencia(request.getPrecioReferencia());
        solicitud.setMostrarPrecio(request.getMostrarPrecio());
        solicitud.setWhatsappContacto(limpiarTexto(request.getWhatsappContacto()));
        solicitud.setInstagramContacto(limpiarTexto(request.getInstagramContacto()));
        solicitud.setEmailContacto(limpiarTexto(request.getEmailContacto()));
        solicitud.setNivel(validarDominio(
                limpiarTexto(request.getNivel()),
                NIVELES_PERMITIDOS,
                "nivel"
        ));
        solicitud.setModalidad(validarDominio(
                limpiarTexto(request.getModalidad()),
                MODALIDADES_PERMITIDAS,
                "modalidad"
        ));
        solicitud.setEnfoque(validarDominio(
                limpiarTexto(request.getEnfoque()),
                ENFOQUES_PERMITIDOS,
                "enfoque"
        ));

        if (request.getDeporteId() != null) {
            solicitud.setDeporte(deporteRepository
                    .findByIdAndActivoTrue(request.getDeporteId())
                    .orElseThrow(() -> new SolicitudCambioInvalidaException(
                            "El deporte propuesto no existe o no esta activo."
                    )));
        }

        aplicarEdadesValidadas(solicitud, request, actividad);
        aplicarUbicacionValidada(solicitud, request, actividad);
        aplicarHorariosValidados(solicitud, request);

        return solicitud;
    }

    /**
     * Las edades se validan sobre el RESULTADO combinado (propuesto +
     * vigente): proponer solo la minima no puede dejarla por encima de
     * la maxima que queda.
     */
    private void aplicarEdadesValidadas(
            SolicitudCambioActividad solicitud,
            SolicitudCambioActividadRequestDTO request,
            Actividad actividad
    ) {
        if (request.getEdadMinima() == null && request.getEdadMaxima() == null) {
            return;
        }

        Integer minResultante = request.getEdadMinima() != null
                ? request.getEdadMinima()
                : actividad.getEdadMinima();
        Integer maxResultante = request.getEdadMaxima() != null
                ? request.getEdadMaxima()
                : actividad.getEdadMaxima();

        if (minResultante != null && maxResultante != null && minResultante > maxResultante) {
            throw new SolicitudCambioInvalidaException(
                    "La edad minima resultante (" + minResultante
                            + ") no puede superar la maxima (" + maxResultante + ")."
            );
        }

        solicitud.setEdadMinima(request.getEdadMinima());
        solicitud.setEdadMaxima(request.getEdadMaxima());
    }

    /**
     * Ubicacion propuesta: direccion y barrio van juntos, y el barrio
     * tiene que ser de la MISMA ciudad de la actividad (cambiar de
     * ciudad no entra en este flujo).
     */
    private void aplicarUbicacionValidada(
            SolicitudCambioActividad solicitud,
            SolicitudCambioActividadRequestDTO request,
            Actividad actividad
    ) {
        String nombre = limpiarTexto(request.getUbicacionNombre());
        String direccion = limpiarTexto(request.getUbicacionDireccion());
        String referencia = limpiarTexto(request.getUbicacionReferencia());
        Long barrioId = request.getUbicacionBarrioId();

        if (nombre == null && direccion == null && referencia == null && barrioId == null) {
            return;
        }

        if (direccion == null || barrioId == null) {
            throw new SolicitudCambioInvalidaException(
                    "Para proponer un cambio de ubicacion, la direccion y el barrio son obligatorios."
            );
        }

        Long ciudadActualId = actividad.getUbicacion() != null
                && actividad.getUbicacion().getCiudad() != null
                ? actividad.getUbicacion().getCiudad().getId()
                : null;

        com.dondeentreno.api.entity.Barrio barrio = barrioRepository.findById(barrioId)
                .filter(b -> Boolean.TRUE.equals(b.getActivo()))
                .orElseThrow(() -> new SolicitudCambioInvalidaException(
                        "El barrio propuesto no existe o no esta activo."
                ));

        if (ciudadActualId != null
                && (barrio.getCiudad() == null
                        || !ciudadActualId.equals(barrio.getCiudad().getId()))) {
            throw new SolicitudCambioInvalidaException(
                    "El barrio propuesto tiene que ser de la misma ciudad de la actividad."
            );
        }

        solicitud.setUbicacionNombre(nombre);
        solicitud.setUbicacionDireccion(direccion);
        solicitud.setUbicacionReferencia(referencia);
        solicitud.setUbicacionBarrio(barrio);
    }

    /**
     * Horarios: reemplazo total con flag explicito. true exige >=1 fila
     * valida (fin > inicio, sin filas exactamente duplicadas); false o
     * ausente ignora la lista.
     */
    private void aplicarHorariosValidados(
            SolicitudCambioActividad solicitud,
            SolicitudCambioActividadRequestDTO request
    ) {
        if (!Boolean.TRUE.equals(request.getCambiaHorarios())) {
            return;
        }

        List<com.dondeentreno.api.dto.SolicitudPublicacionHorarioRequestDTO> propuestos =
                request.getHorarios() != null ? request.getHorarios() : List.of();

        if (propuestos.isEmpty()) {
            throw new SolicitudCambioInvalidaException(
                    "Para cambiar los horarios tenes que proponer al menos uno."
            );
        }

        java.util.Set<String> vistos = new java.util.HashSet<>();
        OffsetDateTime ahora = OffsetDateTime.now();

        solicitud.setCambiaHorarios(Boolean.TRUE);

        for (var propuesto : propuestos) {
            if (!propuesto.getHoraFin().isAfter(propuesto.getHoraInicio())) {
                throw new SolicitudCambioInvalidaException(
                        "Un horario tiene que terminar despues de empezar ("
                                + propuesto.getDiaSemana() + " "
                                + propuesto.getHoraInicio() + "-" + propuesto.getHoraFin() + ")."
                );
            }

            String clave = propuesto.getDiaSemana() + "|"
                    + propuesto.getHoraInicio() + "|" + propuesto.getHoraFin();

            if (!vistos.add(clave)) {
                throw new SolicitudCambioInvalidaException(
                        "Hay horarios exactamente duplicados ("
                                + propuesto.getDiaSemana() + " "
                                + propuesto.getHoraInicio() + "-" + propuesto.getHoraFin() + ")."
                );
            }

            SolicitudCambioHorario horario = new SolicitudCambioHorario();
            horario.setSolicitud(solicitud);
            horario.setDiaSemana(propuesto.getDiaSemana());
            horario.setHoraInicio(propuesto.getHoraInicio());
            horario.setHoraFin(propuesto.getHoraFin());
            horario.setObservacion(limpiarTexto(propuesto.getObservacion()));
            horario.setCreatedAt(ahora);
            horario.setUpdatedAt(ahora);
            solicitud.getHorarios().add(horario);
        }
    }

    private String limpiarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String validarDominio(
            String valor,
            List<String> permitidos,
            String nombreCampo
    ) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.toUpperCase(Locale.ROOT);

        if (!permitidos.contains(normalizado)) {
            throw new SolicitudCambioInvalidaException(
                    "El campo '" + nombreCampo + "' tiene un valor invalido: '"
                            + valor + "'. Valores permitidos: "
                            + String.join(", ", permitidos) + "."
            );
        }

        return normalizado;
    }
}
