package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.EventoDeportivoDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.EventoDeportivo;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.EventoDeportivoRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.InteresEventoRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Eventos deportivos y calendario (script 35, Fase 9).
 *
 * Publica DIRECTO, sin cola de moderación, y por una razón que no
 * tiene vuelta: un evento CADUCA. Un torneo del sábado esperando
 * aprobación hasta el lunes no es un evento moderado, es un evento
 * perdido. El riesgo queda acotado porque para publicar hay que ser
 * publicador activo —ya pasó el filtro humano al publicar su primera
 * actividad— y por atrás lo cubren reportes y ocultar por admin.
 */
@Service
public class EventoDeportivoService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    public static final String ESTADO_PUBLICADO = "PUBLICADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    private static final String ESTADO_OCULTO = "OCULTO_POR_ADMIN";
    private static final String ESTADO_ELIMINADO = "ELIMINADO_POR_PUBLICADOR";
    private static final String ESTADO_IMAGEN_APROBADA = "APROBADA";

    /**
     * Tope de CAMPANITA, no de publicación: cargar la agenda del mes
     * de una sentada es legítimo, pero `NotificacionService` no agrupa
     * y serían cientos de avisos a la misma gente. Los eventos que
     * pasan este tope se publican igual y entran al feed.
     */
    private static final int MAX_NOTIFICACIONES_POR_DIA = 2;

    private static final int MAX_TITULO = 150;
    private static final int MAX_DESCRIPCION = 2000;
    private static final int MAX_PAGINA = 50;
    private static final int MAX_PROXIMOS = 20;
    /** El "sin tope" del calendario: ver `calendario(...)`. */
    private static final int HORIZONTE_ANIOS = 5;

    private final EventoDeportivoRepository eventoRepository;
    private final InteresEventoRepository interesEventoRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final UbicacionRepository ubicacionRepository;
    private final DeporteRepository deporteRepository;
    private final ImagenRepository imagenRepository;
    private final ImagenService imagenService;
    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;
    private final NotificacionService notificacionService;
    private final FeedEventService feedEventService;
    private final EventoSlugService eventoSlugService;

    public EventoDeportivoService(
            EventoDeportivoRepository eventoRepository,
            InteresEventoRepository interesEventoRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            UbicacionRepository ubicacionRepository,
            DeporteRepository deporteRepository,
            ImagenRepository imagenRepository,
            ImagenService imagenService,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository,
            NotificacionService notificacionService,
            FeedEventService feedEventService,
            EventoSlugService eventoSlugService
    ) {
        this.eventoRepository = eventoRepository;
        this.interesEventoRepository = interesEventoRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.deporteRepository = deporteRepository;
        this.imagenRepository = imagenRepository;
        this.imagenService = imagenService;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
        this.notificacionService = notificacionService;
        this.feedEventService = feedEventService;
        this.eventoSlugService = eventoSlugService;
    }

    /* ======================= escritura ======================= */

    @Transactional
    public EventoDeportivoDTO publicar(Long userId, DatosEvento datos) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        String titulo = recortar(exigirTexto(datos.titulo(), "El evento necesita un título."), MAX_TITULO);
        String descripcion = recortar(
                exigirTexto(datos.descripcion(), "Contá de qué se trata el evento."),
                MAX_DESCRIPCION
        );

        if (datos.iniciaAt() == null) {
            throw new FiltroInvalidoException("El evento necesita fecha y hora de inicio.");
        }
        /*
          Un evento que ya pasó no se puede crear: no es un dato
          histórico, es un error de carga (típico: el año).
        */
        if (datos.iniciaAt().isBefore(OffsetDateTime.now())) {
            throw new FiltroInvalidoException("La fecha del evento ya pasó. Revisá el día y la hora.");
        }
        if (datos.terminaAt() != null && !datos.terminaAt().isAfter(datos.iniciaAt())) {
            throw new FiltroInvalidoException("El evento no puede terminar antes de empezar.");
        }
        if (datos.cupo() != null && datos.cupo() <= 0) {
            throw new FiltroInvalidoException("El cupo tiene que ser mayor a cero.");
        }

        Actividad actividad = resolverActividadPropia(perfil.getId(), datos.actividadId());
        Ubicacion ubicacion = resolverUbicacion(perfil.getId(), datos.ubicacionId(), actividad);
        Long deporteId = resolverDeporte(datos.deporteId(), actividad);

        OffsetDateTime ahora = OffsetDateTime.now();

        EventoDeportivo evento = new EventoDeportivo();
        evento.setPerfilPublicadorId(perfil.getId());
        evento.setActividadId(actividad != null ? actividad.getId() : null);
        evento.setDeporteId(deporteId);
        evento.setUbicacionId(ubicacion.getId());
        evento.setImagenId(validarImagenPropia(perfil.getId(), datos.imagenId()));
        evento.setTitulo(titulo);
        evento.setSlug(eventoSlugService.generarSlugUnico(titulo));
        evento.setDescripcion(descripcion);
        evento.setIniciaAt(datos.iniciaAt());
        evento.setTerminaAt(datos.terminaAt());
        evento.setCupo(datos.cupo());
        evento.setEsGratis(Boolean.TRUE.equals(datos.esGratis()));
        evento.setPrecioReferencia(
                Boolean.TRUE.equals(datos.esGratis()) ? null : datos.precioReferencia()
        );
        evento.setMostrarPrecio(datos.mostrarPrecio() == null || datos.mostrarPrecio());
        evento.setEstado(ESTADO_PUBLICADO);
        evento.setCreatedAt(ahora);
        evento.setUpdatedAt(ahora);

        EventoDeportivo guardado = eventoRepository.saveAndFlush(evento);

        /* Al feed de sus seguidores (best-effort, afterCommit). */
        feedEventService.emitirEvento(
                perfil.getId(),
                guardado.getId(),
                guardado.getImagenId(),
                titulo
        );

        /*
          Campanita con tope: ver MAX_NOTIFICACIONES_POR_DIA. El conteo
          es de eventos CREADOS hoy, no de eventos que ocurren hoy.
        */
        long creadosHoy = eventoRepository
                .countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                        perfil.getId(),
                        inicioDelDiaArgentino()
                );

        if (creadosHoy <= MAX_NOTIFICACIONES_POR_DIA) {
            notificacionService.emitirATodos(
                    seguimientoPublicadorRepository.usuarioIdsSeguidoresDe(perfil.getId()),
                    "EVENTO_NUEVO",
                    perfil.getNombre() + " organiza: " + titulo,
                    "/eventos/" + guardado.getSlug()
            );
        }

        return enriquecer(List.of(guardado), null).get(0);
    }

    /**
     * Cancelar NO es borrar: el link ya circuló por WhatsApp, así que
     * el detalle sigue vivo diciendo que se canceló. Sale del
     * calendario, pero no del perfil del publicador.
     */
    @Transactional
    public void cancelar(Long userId, Long eventoId) {
        EventoDeportivo evento = exigirPropio(userId, eventoId);

        evento.setEstado(ESTADO_CANCELADO);
        evento.setUpdatedAt(OffsetDateTime.now());
        eventoRepository.save(evento);
    }

    /** Borrar de verdad (baja lógica): desaparece de todas las vistas. */
    @Transactional
    public void eliminarPropio(Long userId, Long eventoId) {
        EventoDeportivo evento = exigirPropio(userId, eventoId);

        evento.setEstado(ESTADO_ELIMINADO);
        evento.setUpdatedAt(OffsetDateTime.now());
        eventoRepository.save(evento);
    }

    /** El admin lo oculta (moderación reactiva). */
    @Transactional
    public void ocultarPorAdmin(Long eventoId) {
        EventoDeportivo evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el evento."));

        evento.setEstado(ESTADO_OCULTO);
        evento.setUpdatedAt(OffsetDateTime.now());
        eventoRepository.save(evento);
    }

    /** ¿Visible? (para reportarlo). Un cancelado sigue siéndolo. */
    @Transactional(readOnly = true)
    public boolean esVisible(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .filter(evento -> ESTADO_PUBLICADO.equals(evento.getEstado())
                        || ESTADO_CANCELADO.equals(evento.getEstado()))
                .isPresent();
    }

    /* ==================== "me interesa" ==================== */

    /** Idempotente: marcar dos veces no duplica (lo fija el UNIQUE). */
    @Transactional
    public long marcarInteres(Long usuarioId, Long eventoId) {
        validarUsuario(usuarioId);
        exigirVisible(eventoId);

        if (!interesEventoRepository.existsByUsuarioIdAndEventoDeportivoId(usuarioId, eventoId)) {
            com.dondeentreno.api.entity.InteresEvento interes =
                    new com.dondeentreno.api.entity.InteresEvento();
            interes.setUsuarioId(usuarioId);
            interes.setEventoDeportivoId(eventoId);
            interes.setCreatedAt(OffsetDateTime.now());

            try {
                interesEventoRepository.saveAndFlush(interes);
            } catch (org.springframework.dao.DataIntegrityViolationException excepcion) {
                /* Otro request lo marcó en el medio: mismo resultado. */
            }
        }

        return interesEventoRepository.countByEventoDeportivoId(eventoId);
    }

    @Transactional
    public long quitarInteres(Long usuarioId, Long eventoId) {
        validarUsuario(usuarioId);
        exigirVisible(eventoId);

        interesEventoRepository
                .findByUsuarioIdAndEventoDeportivoId(usuarioId, eventoId)
                .ifPresent(interesEventoRepository::delete);

        return interesEventoRepository.countByEventoDeportivoId(eventoId);
    }

    /* ======================= lectura ======================= */

    /**
     * El calendario público. `desde`/`hasta` los resuelve el
     * controller a partir del rango pedido (hoy, finde, semana).
     */
    @Transactional(readOnly = true)
    public PaginaResponseDTO<EventoDeportivoDTO> calendario(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Long ciudadId,
            String ciudadSlug,
            Long barrioId,
            Long deporteId,
            Long usuarioId,
            int page,
            int size
    ) {
        int tamanio = Math.min(Math.max(size, 1), MAX_PAGINA);
        int pagina = Math.max(page, 0);

        OffsetDateTime inicio = desde != null ? desde : OffsetDateTime.now();

        /*
          "Todos los próximos" no tiene tope conceptual, pero el query
          NO acepta un `hasta` nulo: Postgres no puede inferir el tipo
          de un parámetro temporal null y revienta el calendario entero
          con un 500 (lo destapó el IT). Un horizonte de años es, en
          este dominio, exactamente lo mismo que no tener tope.
        */
        Page<EventoDeportivo> resultado = eventoRepository.buscarEnCalendario(
                inicio,
                hasta != null ? hasta : inicio.plusYears(HORIZONTE_ANIOS),
                ciudadId,
                ciudadSlug != null && !ciudadSlug.isBlank() ? ciudadSlug : null,
                barrioId,
                deporteId,
                PageRequest.of(pagina, tamanio)
        );

        return new PaginaResponseDTO<>(
                enriquecer(resultado.getContent(), usuarioId),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isLast()
        );
    }

    @Transactional(readOnly = true)
    public EventoDeportivoDTO obtenerPorSlug(String slug, Long usuarioId) {
        EventoDeportivo evento = eventoRepository.findBySlug(slug)
                .filter(encontrado -> ESTADO_PUBLICADO.equals(encontrado.getEstado())
                        || ESTADO_CANCELADO.equals(encontrado.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El evento solicitado no existe o no está disponible."
                ));

        return enriquecer(List.of(evento), usuarioId).get(0);
    }

    /** Los próximos de un publicador (solapa de su perfil público). */
    @Transactional(readOnly = true)
    public List<EventoDeportivoDTO> proximosDePerfil(Long perfilPublicadorId, Long usuarioId, int limite) {
        return enriquecer(
                eventoRepository.proximosDePerfil(
                        perfilPublicadorId,
                        OffsetDateTime.now(),
                        PageRequest.of(0, Math.min(Math.max(limite, 1), MAX_PROXIMOS))
                ),
                usuarioId
        );
    }

    /** El aviso "hay algo próximo" en el detalle de la actividad. */
    @Transactional(readOnly = true)
    public List<EventoDeportivoDTO> proximosDeActividad(Long actividadId, Long usuarioId, int limite) {
        return enriquecer(
                eventoRepository.proximosDeActividad(
                        actividadId,
                        OffsetDateTime.now(),
                        PageRequest.of(0, Math.min(Math.max(limite, 1), MAX_PROXIMOS))
                ),
                usuarioId
        );
    }

    /** La agenda del publicador autenticado. */
    @Transactional(readOnly = true)
    public List<EventoDeportivoDTO> listarMios(Long userId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        return enriquecer(
                eventoRepository.findByPerfilPublicadorIdAndEstadoNotOrderByIniciaAtDesc(
                        perfil.getId(),
                        ESTADO_ELIMINADO
                ),
                null
        );
    }

    /* ======================= interno ======================= */

    /**
     * Completa los eventos con publicador, deporte, sede, actividad,
     * foto e interesados, TODO en queries batch: un calendario de 20
     * eventos no puede disparar cien consultas.
     */
    private List<EventoDeportivoDTO> enriquecer(List<EventoDeportivo> eventos, Long usuarioId) {
        if (eventos.isEmpty()) {
            return List.of();
        }

        List<Long> perfilIds = idsDe(eventos, EventoDeportivo::getPerfilPublicadorId);
        Map<Long, PerfilPublicador> perfiles = indexar(
                perfilPublicadorRepository.findAllById(perfilIds), PerfilPublicador::getId);
        Map<Long, String> logos = imagenService.obtenerLogosAprobadosPorPerfil(perfilIds);

        Map<Long, Ubicacion> ubicaciones = indexar(
                ubicacionRepository.findAllById(idsDe(eventos, EventoDeportivo::getUbicacionId)),
                Ubicacion::getId);

        Map<Long, Deporte> deportes = indexar(
                deporteRepository.findAllById(idsDe(eventos, EventoDeportivo::getDeporteId)),
                Deporte::getId);

        List<Long> actividadIds = idsDe(eventos, EventoDeportivo::getActividadId);
        Map<Long, Actividad> actividades = actividadIds.isEmpty()
                ? Map.of()
                : indexar(actividadRepository.findAllById(actividadIds), Actividad::getId);

        List<Long> imagenIds = idsDe(eventos, EventoDeportivo::getImagenId);
        Map<Long, Imagen> imagenes = imagenIds.isEmpty()
                ? Map.of()
                : indexar(imagenRepository.findAllById(imagenIds), Imagen::getId);

        List<Long> eventoIds = eventos.stream().map(EventoDeportivo::getId).toList();
        Map<Long, Long> interesados = new HashMap<>();
        for (InteresEventoRepository.ConteoInteresados conteo
                : interesEventoRepository.contarPorEventos(eventoIds)) {
            interesados.put(conteo.getEventoId(), conteo.getCantidad());
        }

        List<Long> conInteresPropio = usuarioId != null
                ? interesEventoRepository.eventoIdsConInteresDe(usuarioId, eventoIds)
                : List.of();

        return eventos.stream().map(evento -> {
            EventoDeportivoDTO dto = new EventoDeportivoDTO();
            dto.setId(evento.getId());
            dto.setSlug(evento.getSlug());
            dto.setTitulo(evento.getTitulo());
            dto.setDescripcion(evento.getDescripcion());
            dto.setIniciaAt(evento.getIniciaAt());
            dto.setTerminaAt(evento.getTerminaAt());
            dto.setCupo(evento.getCupo());
            dto.setEsGratis(evento.getEsGratis());
            dto.setMostrarPrecio(evento.getMostrarPrecio());
            dto.setEstado(evento.getEstado());

            /* El precio oculto no viaja al cliente, no se esconde en el front. */
            if (Boolean.TRUE.equals(evento.getMostrarPrecio())) {
                dto.setPrecioReferencia(evento.getPrecioReferencia());
            }

            PerfilPublicador perfil = perfiles.get(evento.getPerfilPublicadorId());
            dto.setPerfilPublicadorId(evento.getPerfilPublicadorId());
            if (perfil != null) {
                dto.setPerfilNombre(perfil.getNombre());
                dto.setPerfilSlug(perfil.getSlug());
                dto.setWhatsappContacto(perfil.getWhatsapp());
            }
            dto.setPerfilLogoUrl(logos.get(evento.getPerfilPublicadorId()));

            Deporte deporte = deportes.get(evento.getDeporteId());
            dto.setDeporteId(evento.getDeporteId());
            if (deporte != null) {
                dto.setDeporteNombre(deporte.getNombre());
                dto.setDeporteSlug(deporte.getSlug());
            }

            Ubicacion ubicacion = ubicaciones.get(evento.getUbicacionId());
            if (ubicacion != null) {
                dto.setSedeNombre(ubicacion.getNombre());
                dto.setDireccion(ubicacion.getDireccion());
                dto.setLatitud(ubicacion.getLatitud());
                dto.setLongitud(ubicacion.getLongitud());
                if (ubicacion.getCiudad() != null) {
                    dto.setCiudadNombre(ubicacion.getCiudad().getNombre());
                    dto.setCiudadSlug(ubicacion.getCiudad().getSlug());
                }
                if (ubicacion.getBarrio() != null) {
                    dto.setBarrioNombre(ubicacion.getBarrio().getNombre());
                }
            }

            Actividad actividad = evento.getActividadId() != null
                    ? actividades.get(evento.getActividadId())
                    : null;
            if (actividad != null) {
                dto.setActividadId(actividad.getId());
                dto.setActividadTitulo(actividad.getTitulo());
                dto.setActividadSlug(actividad.getSlug());
            }

            Imagen imagen = evento.getImagenId() != null
                    ? imagenes.get(evento.getImagenId())
                    : null;
            if (imagen != null && ImagenMapper.esUrlPublicable(imagen.getUrl())) {
                dto.setImagenId(imagen.getId());
                dto.setImagenUrl(imagen.getUrl());
            }

            dto.setCantidadInteresados(interesados.getOrDefault(evento.getId(), 0L));
            dto.setMeInteresa(usuarioId != null && conInteresPropio.contains(evento.getId()));

            return dto;
        }).toList();
    }

    private <T> List<Long> idsDe(
            List<EventoDeportivo> eventos,
            java.util.function.Function<EventoDeportivo, Long> extractor
    ) {
        return eventos.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private <T> Map<Long, T> indexar(
            List<T> elementos,
            java.util.function.Function<T, Long> id
    ) {
        Map<Long, T> mapa = new HashMap<>();
        elementos.forEach(elemento -> mapa.put(id.apply(elemento), elemento));
        return mapa;
    }

    /**
     * La actividad tiene que ser SUYA y estar publicada. Acá sí se
     * falla en vez de ignorar (a diferencia de la foto de la novedad):
     * el evento hereda de ella deporte y sede, así que aceptar una
     * actividad ajena en silencio dejaría el evento en otro lado.
     */
    private Actividad resolverActividadPropia(Long perfilId, Long actividadId) {
        if (actividadId == null) {
            return null;
        }

        return actividadRepository.findById(actividadId)
                .filter(actividad -> actividad.getPerfilPublicador() != null
                        && perfilId.equals(actividad.getPerfilPublicador().getId()))
                .filter(actividad -> Boolean.TRUE.equals(actividad.getActiva())
                        && "PUBLICADA".equals(actividad.getEstadoPublicacion())
                        && actividad.getDeletedAt() == null)
                .orElseThrow(() -> new FiltroInvalidoException(
                        "Esa actividad no es tuya o no está publicada."
                ));
    }

    private Ubicacion resolverUbicacion(Long perfilId, Long ubicacionId, Actividad actividad) {
        /* Si no eligió sede pero el evento cuelga de una actividad, la de ella. */
        if (ubicacionId == null && actividad != null && actividad.getUbicacion() != null) {
            return actividad.getUbicacion();
        }

        if (ubicacionId == null) {
            throw new FiltroInvalidoException("Elegí en qué sede es el evento.");
        }

        return ubicacionRepository.findById(ubicacionId)
                .filter(ubicacion -> ubicacion.getPerfilPublicador() != null
                        && perfilId.equals(ubicacion.getPerfilPublicador().getId()))
                .filter(ubicacion -> Boolean.TRUE.equals(ubicacion.getActiva())
                        && ubicacion.getDeletedAt() == null)
                .orElseThrow(() -> new FiltroInvalidoException(
                        "Esa sede no es tuya o no está disponible."
                ));
    }

    private Long resolverDeporte(Long deporteId, Actividad actividad) {
        if (deporteId != null) {
            return deporteRepository.findById(deporteId)
                    .filter(deporte -> Boolean.TRUE.equals(deporte.getActivo()))
                    .map(Deporte::getId)
                    .orElseThrow(() -> new FiltroInvalidoException("Ese deporte no existe."));
        }

        if (actividad != null && actividad.getDeporte() != null) {
            return actividad.getDeporte().getId();
        }

        throw new FiltroInvalidoException("Elegí de qué deporte es el evento.");
    }

    /**
     * La foto tiene que ser del publicador y estar publicada. Si manda
     * una ajena, el evento sale SIN foto en vez de fallar (mismo
     * criterio que la novedad de la Fase 8).
     */
    private Long validarImagenPropia(Long perfilId, Long imagenId) {
        if (imagenId == null) {
            return null;
        }

        return imagenRepository.findById(imagenId)
                .filter(imagen -> Boolean.TRUE.equals(imagen.getActiva()))
                .filter(imagen -> ESTADO_IMAGEN_APROBADA.equals(imagen.getEstadoModeracion()))
                .filter(imagen -> esDelPublicador(imagen, perfilId))
                .map(Imagen::getId)
                .orElse(null);
    }

    private boolean esDelPublicador(Imagen imagen, Long perfilId) {
        if (imagen.getPerfilPublicador() != null) {
            return perfilId.equals(imagen.getPerfilPublicador().getId());
        }

        return imagen.getActividad() != null
                && imagen.getActividad().getPerfilPublicador() != null
                && perfilId.equals(imagen.getActividad().getPerfilPublicador().getId());
    }

    /** 404 y no 403: no se delata que el evento ajeno existe. */
    private EventoDeportivo exigirPropio(Long userId, Long eventoId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        return eventoRepository.findById(eventoId)
                .filter(evento -> evento.getPerfilPublicadorId().equals(perfil.getId()))
                .filter(evento -> !ESTADO_ELIMINADO.equals(evento.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el evento."));
    }

    private EventoDeportivo exigirVisible(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .filter(evento -> ESTADO_PUBLICADO.equals(evento.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El evento solicitado no existe o no está disponible."
                ));
    }

    private PerfilPublicador obtenerPerfil(Long userId) {
        validarUsuario(userId);

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Perfil publicador no encontrado."
                ));
    }

    private void validarUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }

    private String exigirTexto(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw new FiltroInvalidoException(mensaje);
        }

        return texto.trim();
    }

    private String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private OffsetDateTime inicioDelDiaArgentino() {
        return LocalDate.now(ZONA_ARGENTINA)
                .atStartOfDay(ZONA_ARGENTINA)
                .toOffsetDateTime();
    }

    /** Lo que llega del controller para crear un evento. */
    public record DatosEvento(
            String titulo,
            String descripcion,
            OffsetDateTime iniciaAt,
            OffsetDateTime terminaAt,
            Long actividadId,
            Long ubicacionId,
            Long deporteId,
            Long imagenId,
            Integer cupo,
            Boolean esGratis,
            BigDecimal precioReferencia,
            Boolean mostrarPrecio
    ) {
    }
}
