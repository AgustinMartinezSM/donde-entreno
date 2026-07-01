package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAprobacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionCambiarEstadoRequestDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.mapper.SolicitudPublicacionAdminMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
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
    private static final String ESTADO_PUBLICACION_PUBLICADA = "PUBLICADA";
    private static final String MENSAJE_DATOS_INSUFICIENTES =
            "La solicitud no tiene datos suficientes para crear la actividad.";
    private static final String MENSAJE_HORARIOS_INVALIDOS =
            "La solicitud no tiene horarios validos.";
    private static final int SLUG_MAX_LENGTH = 180;

    private final SolicitudPublicacionRepository solicitudPublicacionRepository;
    private final SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActividadRepository actividadRepository;
    private final HorarioActividadRepository horarioActividadRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final UbicacionRepository ubicacionRepository;
    private final DeporteRepository deporteRepository;
    private final CiudadRepository ciudadRepository;
    private final BarrioRepository barrioRepository;

    public SolicitudPublicacionAdminService(
            SolicitudPublicacionRepository solicitudPublicacionRepository,
            SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository,
            UsuarioRepository usuarioRepository,
            ActividadRepository actividadRepository,
            HorarioActividadRepository horarioActividadRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            UbicacionRepository ubicacionRepository,
            DeporteRepository deporteRepository,
            CiudadRepository ciudadRepository,
            BarrioRepository barrioRepository
    ) {
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
        this.solicitudPublicacionHorarioRepository = solicitudPublicacionHorarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.actividadRepository = actividadRepository;
        this.horarioActividadRepository = horarioActividadRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.deporteRepository = deporteRepository;
        this.ciudadRepository = ciudadRepository;
        this.barrioRepository = barrioRepository;
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

    @Transactional
    public SolicitudPublicacionAprobacionResponseDTO aprobarSolicitud(Long id, Long usuarioAutenticadoId) {
        SolicitudPublicacion solicitud = buscarSolicitudActivaParaActualizar(id);
        validarEstadoAprobable(solicitud);

        Usuario usuarioAdmin = buscarUsuarioRevisor(usuarioAutenticadoId);
        Deporte deporte = resolverDeporte(solicitud);
        Ciudad ciudad = resolverCiudad(solicitud);
        Barrio barrio = resolverBarrio(solicitud, ciudad);
        List<SolicitudPublicacionHorario> horariosSolicitud = buscarHorariosValidos(solicitud.getId());
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfilPublicador = obtenerOCrearPerfilPublicador(solicitud, usuarioAdmin, ahora);
        Ubicacion ubicacion = obtenerOCrearUbicacion(solicitud, perfilPublicador, ciudad, barrio, ahora);
        Actividad actividad = crearActividad(solicitud, perfilPublicador, deporte, ubicacion, ahora);
        crearHorariosActividad(actividad, horariosSolicitud, ahora);

        if (solicitud.getRevisionIniciadaAt() == null) {
            solicitud.setRevisionIniciadaAt(ahora);
        }
        solicitud.setEstado(ESTADO_APROBADA);
        solicitud.setActividadGenerada(actividad);
        solicitud.setRevisadoPorUsuario(usuarioAdmin);
        solicitud.setRevisionFinalizadaAt(ahora);
        solicitud.setUpdatedAt(ahora);
        solicitud.setMotivoRechazo(null);

        SolicitudPublicacion solicitudGuardada = solicitudPublicacionRepository.save(solicitud);

        return new SolicitudPublicacionAprobacionResponseDTO(
                solicitudGuardada.getId(),
                solicitudGuardada.getEstado(),
                actividad.getId(),
                actividad.getSlug(),
                actividad.getTitulo(),
                "La solicitud fue aprobada correctamente y se creo la actividad."
        );
    }

    private SolicitudPublicacion buscarSolicitudActivaParaActualizar(Long id) {
        return solicitudPublicacionRepository.findByIdAndDeletedAtIsNullForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de publicacion no encontrada."));
    }

    private SolicitudPublicacion buscarSolicitudActiva(Long id) {
        return solicitudPublicacionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de publicacion no encontrada."));
    }

    private void validarEstadoAprobable(SolicitudPublicacion solicitud) {
        if (solicitud.getActividadGenerada() != null) {
            throw new SolicitudPublicacionInvalidaException("La solicitud ya tiene una actividad generada.");
        }

        String estado = solicitud.getEstado();
        if (ESTADO_APROBADA.equals(estado)) {
            throw new SolicitudPublicacionInvalidaException("La solicitud ya fue aprobada.");
        }

        if (ESTADO_RECHAZADA.equals(estado)) {
            throw new SolicitudPublicacionInvalidaException("No se puede aprobar una solicitud rechazada.");
        }

        if (!ESTADO_PENDIENTE.equals(estado) && !ESTADO_EN_REVISION.equals(estado)) {
            throw new SolicitudPublicacionInvalidaException("La solicitud no puede aprobarse en su estado actual.");
        }
    }

    private Deporte resolverDeporte(SolicitudPublicacion solicitud) {
        Deporte deporteSolicitud = solicitud.getDeporte();
        if (deporteSolicitud == null || deporteSolicitud.getId() == null) {
            throw new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES);
        }

        return deporteRepository.findByIdAndActivoTrue(deporteSolicitud.getId())
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES));
    }

    private Ciudad resolverCiudad(SolicitudPublicacion solicitud) {
        Ciudad ciudadSolicitud = solicitud.getCiudad();
        if (ciudadSolicitud == null || ciudadSolicitud.getId() == null) {
            throw new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES);
        }

        return ciudadRepository.findByIdAndActivaTrue(ciudadSolicitud.getId())
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES));
    }

    private Barrio resolverBarrio(SolicitudPublicacion solicitud, Ciudad ciudad) {
        Barrio barrioSolicitud = solicitud.getBarrio();
        if (barrioSolicitud == null || barrioSolicitud.getId() == null || ciudad.getId() == null) {
            throw new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES);
        }

        return barrioRepository.findByIdAndActivoTrueAndCiudad_Id(barrioSolicitud.getId(), ciudad.getId())
                .orElseThrow(() -> new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES));
    }

    private List<SolicitudPublicacionHorario> buscarHorariosValidos(Long solicitudId) {
        List<SolicitudPublicacionHorario> horarios = buscarHorariosOrdenados(solicitudId);

        if (horarios.isEmpty() || horarios.stream().anyMatch(this::esHorarioInvalido)) {
            throw new SolicitudPublicacionInvalidaException(MENSAJE_HORARIOS_INVALIDOS);
        }

        return horarios;
    }

    private boolean esHorarioInvalido(SolicitudPublicacionHorario horario) {
        return horario.getDiaSemana() == null
                || horario.getDiaSemana().isBlank()
                || horario.getHoraInicio() == null
                || horario.getHoraFin() == null
                || !horario.getHoraInicio().isBefore(horario.getHoraFin());
    }

    private PerfilPublicador obtenerOCrearPerfilPublicador(
            SolicitudPublicacion solicitud,
            Usuario usuarioAdmin,
            OffsetDateTime ahora
    ) {
        String nombre = exigirTexto(solicitud.getNombrePublicador());
        String tipoPublicador = exigirTexto(solicitud.getTipoPublicador());

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        usuarioAdmin.getId(),
                        tipoPublicador,
                        nombre
                )
                .orElseGet(() -> crearPerfilPublicador(solicitud, usuarioAdmin, nombre, tipoPublicador, ahora));
    }

    private PerfilPublicador crearPerfilPublicador(
            SolicitudPublicacion solicitud,
            Usuario usuarioAdmin,
            String nombre,
            String tipoPublicador,
            OffsetDateTime ahora
    ) {
        PerfilPublicador perfilPublicador = new PerfilPublicador();
        perfilPublicador.setUsuario(usuarioAdmin);
        perfilPublicador.setNombre(nombre);
        perfilPublicador.setTipoPublicador(tipoPublicador);
        perfilPublicador.setEmailContacto(normalizarTexto(solicitud.getEmail()));
        perfilPublicador.setWhatsapp(obtenerWhatsappContacto(solicitud));
        perfilPublicador.setInstagram(normalizarTexto(solicitud.getInstagram()));
        perfilPublicador.setActivo(true);
        perfilPublicador.setVerificado(false);
        perfilPublicador.setCreatedAt(ahora);
        perfilPublicador.setUpdatedAt(ahora);
        return perfilPublicadorRepository.save(perfilPublicador);
    }

    private Ubicacion obtenerOCrearUbicacion(
            SolicitudPublicacion solicitud,
            PerfilPublicador perfilPublicador,
            Ciudad ciudad,
            Barrio barrio,
            OffsetDateTime ahora
    ) {
        String nombreLugar = normalizarTexto(solicitud.getNombreLugar());
        String nombre = nombreLugar == null
                ? exigirTexto(solicitud.getNombrePublicador())
                : nombreLugar;
        String direccion = exigirTexto(solicitud.getDireccion());

        return ubicacionRepository
                .findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
                        perfilPublicador.getId(),
                        ciudad.getId(),
                        barrio.getId(),
                        nombre,
                        direccion
                )
                .orElseGet(() -> crearUbicacion(
                        solicitud,
                        perfilPublicador,
                        ciudad,
                        barrio,
                        nombre,
                        direccion,
                        ahora
                ));
    }

    private Ubicacion crearUbicacion(
            SolicitudPublicacion solicitud,
            PerfilPublicador perfilPublicador,
            Ciudad ciudad,
            Barrio barrio,
            String nombre,
            String direccion,
            OffsetDateTime ahora
    ) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfilPublicador);
        ubicacion.setCiudad(ciudad);
        ubicacion.setBarrio(barrio);
        ubicacion.setNombre(nombre);
        ubicacion.setDireccion(direccion);
        ubicacion.setReferencia(normalizarTexto(solicitud.getReferenciaUbicacion()));
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);
        return ubicacionRepository.save(ubicacion);
    }

    private Actividad crearActividad(
            SolicitudPublicacion solicitud,
            PerfilPublicador perfilPublicador,
            Deporte deporte,
            Ubicacion ubicacion,
            OffsetDateTime ahora
    ) {
        String titulo = exigirTexto(solicitud.getNombreActividad());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfilPublicador);
        actividad.setDeporte(deporte);
        actividad.setUbicacion(ubicacion);
        actividad.setTitulo(titulo);
        actividad.setSlug(generarSlugUnico(titulo));
        actividad.setDescripcion(exigirTexto(solicitud.getDescripcion()));
        actividad.setEdadMinima(solicitud.getEdadMinima());
        actividad.setEdadMaxima(solicitud.getEdadMaxima());
        actividad.setNivel(exigirTexto(solicitud.getNivel()));
        actividad.setEnfoque(exigirTexto(solicitud.getEnfoque()));
        actividad.setModalidad(exigirTexto(solicitud.getModalidad()));
        actividad.setPrecioReferencia(solicitud.getPrecioReferencia());
        actividad.setMostrarPrecio(Boolean.TRUE.equals(solicitud.getMostrarPrecio()));
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto(obtenerWhatsappContacto(solicitud));
        actividad.setInstagramContacto(normalizarTexto(solicitud.getInstagram()));
        actividad.setEmailContacto(normalizarTexto(solicitud.getEmail()));
        actividad.setEstadoPublicacion(ESTADO_PUBLICACION_PUBLICADA);
        actividad.setMotivoRechazo(null);
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);
        return actividadRepository.save(actividad);
    }

    private void crearHorariosActividad(
            Actividad actividad,
            List<SolicitudPublicacionHorario> horariosSolicitud,
            OffsetDateTime ahora
    ) {
        List<HorarioActividad> horariosActividad = horariosSolicitud.stream()
                .map(horarioSolicitud -> crearHorarioActividad(actividad, horarioSolicitud, ahora))
                .toList();

        horarioActividadRepository.saveAll(horariosActividad);
    }

    private HorarioActividad crearHorarioActividad(
            Actividad actividad,
            SolicitudPublicacionHorario horarioSolicitud,
            OffsetDateTime ahora
    ) {
        HorarioActividad horarioActividad = new HorarioActividad();
        horarioActividad.setActividad(actividad);
        horarioActividad.setDiaSemana(horarioSolicitud.getDiaSemana().trim());
        horarioActividad.setHoraInicio(horarioSolicitud.getHoraInicio());
        horarioActividad.setHoraFin(horarioSolicitud.getHoraFin());
        horarioActividad.setObservacion(normalizarTexto(horarioSolicitud.getObservacion()));
        horarioActividad.setActivo(true);
        horarioActividad.setCreatedAt(ahora);
        horarioActividad.setUpdatedAt(ahora);
        return horarioActividad;
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

    private String generarSlugUnico(String titulo) {
        String base = generarBaseSlug(titulo);
        String slug = limitarSlug(base, "");
        int contador = 2;

        while (actividadRepository.existsBySlug(slug)) {
            String sufijo = "-" + contador;
            slug = limitarSlug(base, sufijo) + sufijo;
            contador++;
        }

        return slug;
    }

    private String generarBaseSlug(String texto) {
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (normalizado.isBlank()) {
            return "actividad";
        }

        return normalizado;
    }

    private String limitarSlug(String base, String sufijo) {
        int longitudMaxima = SLUG_MAX_LENGTH - sufijo.length();
        String slug = base.length() <= longitudMaxima
                ? base
                : base.substring(0, longitudMaxima);

        slug = slug.replaceAll("-+$", "");
        if (slug.isBlank()) {
            return "actividad";
        }

        return slug;
    }

    private String exigirTexto(String texto) {
        String textoNormalizado = normalizarTexto(texto);
        if (textoNormalizado == null) {
            throw new SolicitudPublicacionInvalidaException(MENSAJE_DATOS_INSUFICIENTES);
        }

        return textoNormalizado;
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }

    private String obtenerWhatsappContacto(SolicitudPublicacion solicitud) {
        String whatsappNormalizado = normalizarTexto(solicitud.getWhatsappNormalizado());
        if (whatsappNormalizado != null) {
            return whatsappNormalizado;
        }

        return normalizarTexto(solicitud.getWhatsapp());
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
