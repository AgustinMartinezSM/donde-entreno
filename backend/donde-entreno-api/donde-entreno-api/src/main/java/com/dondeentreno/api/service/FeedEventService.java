package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.FeedEventDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.FeedEvent;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.Novedad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.FeedEventRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.NovedadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Feed de hechos de los publicadores (script 32, Fase 6).
 *
 * La emisión es best-effort con el MISMO contrato que
 * NotificacionService: un fallo del feed jamás puede voltear el hecho
 * real que lo originó (aprobar una actividad, publicar una foto).
 */
@Service
public class FeedEventService {

    private static final Logger log = LoggerFactory.getLogger(FeedEventService.class);

    /** Catálogo abierto: cada fase suma tipos, por eso no hay CHECK. */
    public static final String TIPO_ACTIVIDAD_NUEVA = "ACTIVIDAD_NUEVA";
    public static final String TIPO_FOTOS_NUEVAS = "FOTOS_NUEVAS";
    public static final String TIPO_ACTIVIDAD_ACTUALIZADA = "ACTIVIDAD_ACTUALIZADA";
    /* Fase 8: sin migración, porque feed_event.tipo no tiene CHECK. */
    public static final String TIPO_NOVEDAD = "NOVEDAD";

    private static final int MAX_RESUMEN = 200;
    private static final int MAX_PAGINA = 50;
    private static final String ESTADO_APROBADA = "APROBADA";

    private final FeedEventRepository feedEventRepository;
    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final ImagenRepository imagenRepository;
    private final ImagenService imagenService;
    private final NovedadRepository novedadRepository;

    public FeedEventService(
            FeedEventRepository feedEventRepository,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            ImagenRepository imagenRepository,
            ImagenService imagenService,
            NovedadRepository novedadRepository
    ) {
        this.novedadRepository = novedadRepository;
        this.feedEventRepository = feedEventRepository;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.imagenRepository = imagenRepository;
        this.imagenService = imagenService;
    }

    /**
     * Registra un hecho. Best-effort: nunca lanza.
     *
     * Se emite DESPUÉS del commit del flujo que lo origina, y no en una
     * transacción paralela. La razón es concreta y costó un 500 en los
     * ITs: el evento referencia por FK a la actividad/imagen/perfil que
     * la transacción de negocio todavía no confirmó, así que una
     * transacción nueva —otra conexión— NO las ve y la FK explota. Y
     * como esa violación aparece al hacer commit, el try/catch de acá
     * adentro ni siquiera la atrapa: rompía la subida de fotos entera.
     *
     * Con afterCommit las filas referenciadas ya existen, y si el
     * negocio hace rollback el evento simplemente no se emite, que es
     * exactamente lo que se quiere.
     */
    public void emitir(
            String tipo,
            Long perfilPublicadorId,
            Long actividadId,
            Long imagenId,
            String resumen
    ) {
        if (tipo == null || perfilPublicadorId == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            guardar(tipo, perfilPublicadorId, actividadId, imagenId, resumen);
                        }
                    }
            );
            return;
        }

        /* Sin transacción en curso (tests, jobs): directo. */
        guardar(tipo, perfilPublicadorId, actividadId, imagenId, resumen);
    }

    /**
     * El hecho "contó algo" (Fase 8). Overload y no un parámetro más en
     * `emitir` para no tocar a los cuatro llamadores que ya existen.
     */
    public void emitirNovedad(
            Long perfilPublicadorId,
            Long novedadId,
            Long imagenId,
            String resumen
    ) {
        if (perfilPublicadorId == null || novedadId == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            guardarConNovedad(perfilPublicadorId, novedadId, imagenId, resumen);
                        }
                    }
            );
            return;
        }

        guardarConNovedad(perfilPublicadorId, novedadId, imagenId, resumen);
    }

    void guardarConNovedad(
            Long perfilPublicadorId,
            Long novedadId,
            Long imagenId,
            String resumen
    ) {
        try {
            FeedEvent evento = new FeedEvent();
            evento.setTipo(TIPO_NOVEDAD);
            evento.setPerfilPublicadorId(perfilPublicadorId);
            evento.setNovedadId(novedadId);
            evento.setImagenId(imagenId);
            evento.setResumen(recortar(resumen));
            evento.setCreatedAt(OffsetDateTime.now());

            feedEventRepository.saveAndFlush(evento);
        } catch (RuntimeException excepcion) {
            log.warn("FEED_EVENT_NO_EMITIDO tipo={} perfil={}: {}",
                    TIPO_NOVEDAD, perfilPublicadorId, excepcion.getMessage());
        }
    }

    /**
     * El guardado real, tragándose todo: a esta altura el hecho de
     * negocio ya está confirmado y nada de lo que pase acá puede
     * afectarlo.
     *
     * Sin `@Transactional` a propósito: se llama desde `emitir` (mismo
     * bean), así que el proxy no lo interceptaría igual. No hace falta:
     * `saveAndFlush` abre su propia transacción cuando no hay ninguna
     * activa —el caso de afterCommit— y su commit ocurre DENTRO de
     * esta llamada, o sea dentro del try.
     */
    void guardar(
            String tipo,
            Long perfilPublicadorId,
            Long actividadId,
            Long imagenId,
            String resumen
    ) {
        try {
            FeedEvent evento = new FeedEvent();
            evento.setTipo(tipo);
            evento.setPerfilPublicadorId(perfilPublicadorId);
            evento.setActividadId(actividadId);
            evento.setImagenId(imagenId);
            evento.setResumen(recortar(resumen));
            evento.setCreatedAt(OffsetDateTime.now());

            feedEventRepository.saveAndFlush(evento);
        } catch (RuntimeException excepcion) {
            log.warn("FEED_EVENT_NO_EMITIDO tipo={} perfil={}: {}",
                    tipo, perfilPublicadorId, excepcion.getMessage());
        }
    }

    /**
     * El feed del usuario: los hechos de los publicadores que sigue,
     * PAGINADO (la V1 cortaba en 20 sin forma de pedir más).
     *
     * Mismo molde que NotificacionService.listar, incluido el saneo
     * inline de page/size que nunca lanza por parámetros feos.
     */
    @Transactional(readOnly = true)
    public PaginaResponseDTO<FeedEventDTO> listarParaUsuario(
            Long usuarioId,
            int page,
            int size
    ) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        List<Long> perfilesSeguidos = seguimientoPublicadorRepository
                .findByUsuario_IdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(seguimiento -> seguimiento.getPerfilPublicador().getId())
                .toList();

        int tamanio = Math.min(Math.max(size, 1), MAX_PAGINA);
        int pagina = Math.max(page, 0);

        /* Sin seguidos no se toca la tabla de eventos. */
        if (perfilesSeguidos.isEmpty()) {
            return new PaginaResponseDTO<>(List.of(), pagina, tamanio, 0L, 0, true);
        }

        Page<FeedEvent> paginaEventos = feedEventRepository
                .findByPerfilPublicadorIdInOrderByCreatedAtDesc(
                        perfilesSeguidos,
                        PageRequest.of(pagina, tamanio)
                );

        return new PaginaResponseDTO<>(
                enriquecer(paginaEventos.getContent()),
                paginaEventos.getNumber(),
                paginaEventos.getSize(),
                paginaEventos.getTotalElements(),
                paginaEventos.getTotalPages(),
                paginaEventos.isLast()
        );
    }

    /**
     * Completa cada evento con la identidad del publicador y los datos
     * de la actividad y la foto, todo en queries BATCH: un feed de 20
     * eventos no puede disparar 60 consultas.
     */
    private List<FeedEventDTO> enriquecer(List<FeedEvent> eventos) {
        if (eventos.isEmpty()) {
            return List.of();
        }

        List<Long> perfilIds = eventos.stream()
                .map(FeedEvent::getPerfilPublicadorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PerfilPublicador> perfiles = perfilPublicadorRepository
                .findAllById(perfilIds).stream()
                .collect(HashMap::new, (mapa, perfil) -> mapa.put(perfil.getId(), perfil), HashMap::putAll);
        Map<Long, String> logos = imagenService.obtenerLogosAprobadosPorPerfil(perfilIds);

        List<Long> actividadIds = eventos.stream()
                .map(FeedEvent::getActividadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Actividad> actividades = actividadIds.isEmpty()
                ? Map.of()
                : actividadRepository.findAllById(actividadIds).stream()
                        .collect(HashMap::new,
                                (mapa, actividad) -> mapa.put(actividad.getId(), actividad),
                                HashMap::putAll);

        /* Imagen PRINCIPAL de esas actividades, en un solo query. */
        Map<Long, String> principalPorActividad = new HashMap<>();
        if (!actividadIds.isEmpty()) {
            for (Imagen imagen : imagenRepository
                    .findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
                            ESTADO_APROBADA,
                            "PRINCIPAL",
                            actividadIds
                    )) {
                if (imagen.getActividad() != null
                        && ImagenMapper.esUrlPublicable(imagen.getUrl())) {
                    principalPorActividad.putIfAbsent(
                            imagen.getActividad().getId(),
                            imagen.getUrl()
                    );
                }
            }
        }

        List<Long> imagenIds = eventos.stream()
                .map(FeedEvent::getImagenId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Imagen> imagenes = imagenIds.isEmpty()
                ? Map.of()
                : imagenRepository.findAllById(imagenIds).stream()
                        .collect(HashMap::new,
                                (mapa, imagen) -> mapa.put(imagen.getId(), imagen),
                                HashMap::putAll);

        /*
          Las novedades del canal (Fase 8). Solo las VISIBLES entran al
          mapa, y el evento cuya novedad no está en él se CAE del feed:
          ocultarla por admin tiene que sacarla de todos lados, no solo
          del perfil. El total de la página queda un pelo alto, igual
          que con cualquier filtro de moderación posterior al query.
        */
        List<Long> novedadIds = eventos.stream()
                .map(FeedEvent::getNovedadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Novedad> novedades = novedadIds.isEmpty()
                ? Map.of()
                : novedadRepository.findAllById(novedadIds).stream()
                        .filter(novedad -> NovedadService.ESTADO_VISIBLE.equals(novedad.getEstado()))
                        .collect(HashMap::new,
                                (mapa, novedad) -> mapa.put(novedad.getId(), novedad),
                                HashMap::putAll);

        return eventos.stream().filter(evento ->
                evento.getNovedadId() == null || novedades.containsKey(evento.getNovedadId())
        ).map(evento -> {
            FeedEventDTO dto = new FeedEventDTO();
            dto.setId(evento.getId());
            dto.setTipo(evento.getTipo());
            dto.setResumen(evento.getResumen());
            dto.setCreatedAt(evento.getCreatedAt());

            dto.setPerfilPublicadorId(evento.getPerfilPublicadorId());
            PerfilPublicador perfil = perfiles.get(evento.getPerfilPublicadorId());
            if (perfil != null) {
                dto.setPerfilNombre(perfil.getNombre());
                dto.setPerfilSlug(perfil.getSlug());
            }
            dto.setPerfilLogoUrl(logos.get(evento.getPerfilPublicadorId()));

            Actividad actividad = evento.getActividadId() != null
                    ? actividades.get(evento.getActividadId())
                    : null;
            if (actividad != null) {
                dto.setActividadId(actividad.getId());
                dto.setActividadTitulo(actividad.getTitulo());
                dto.setActividadSlug(actividad.getSlug());
                dto.setActividadImagenUrl(principalPorActividad.get(actividad.getId()));
            }

            Imagen imagen = evento.getImagenId() != null
                    ? imagenes.get(evento.getImagenId())
                    : null;
            if (imagen != null && ImagenMapper.esUrlPublicable(imagen.getUrl())) {
                dto.setImagenId(imagen.getId());
                dto.setImagenUrl(imagen.getUrl());
            }

            Novedad novedad = evento.getNovedadId() != null
                    ? novedades.get(evento.getNovedadId())
                    : null;
            if (novedad != null) {
                dto.setNovedadId(novedad.getId());
                /* El texto completo: el resumen es para el log, no para leer. */
                dto.setNovedadTexto(novedad.getTexto());
            }

            return dto;
        }).toList();
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }

        return texto.length() <= MAX_RESUMEN ? texto : texto.substring(0, MAX_RESUMEN);
    }
}
