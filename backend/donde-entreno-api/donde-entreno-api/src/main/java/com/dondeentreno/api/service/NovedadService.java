package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.NovedadDTO;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.MeGustaNovedad;
import com.dondeentreno.api.entity.Novedad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaNovedadRepository;
import com.dondeentreno.api.repository.NovedadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Canal de novedades del publicador (script 34, Fase 8).
 *
 * Publica DIRECTO y se modera por reportes, igual que el resto de lo
 * social desde la Fase 4. Es broadcast: solo el publicador escribe.
 */
@Service
public class NovedadService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    public static final String ESTADO_VISIBLE = "VISIBLE";
    private static final String ESTADO_OCULTA = "OCULTA_POR_ADMIN";
    private static final String ESTADO_ELIMINADA = "ELIMINADA_POR_PUBLICADOR";
    private static final String ESTADO_IMAGEN_APROBADA = "APROBADA";

    /**
     * Tope del canal. Hasta esta fase NO existía ningún límite para el
     * rol publicador —todos los topes eran sobre el usuario que
     * consume—, y un canal sin tope puede inundar el feed de sus
     * seguidores: el costo de equivocarse lo paga quien lo sigue.
     */
    private static final int MAX_NOVEDADES_POR_DIA = 3;

    private static final int MAX_TEXTO = 1000;
    private static final int MAX_PAGINA = 50;

    private final NovedadRepository novedadRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ImagenRepository imagenRepository;
    private final ImagenService imagenService;
    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;
    private final NotificacionService notificacionService;
    private final FeedEventService feedEventService;
    private final MeGustaNovedadRepository meGustaNovedadRepository;

    public NovedadService(
            NovedadRepository novedadRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ImagenRepository imagenRepository,
            ImagenService imagenService,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository,
            NotificacionService notificacionService,
            FeedEventService feedEventService,
            MeGustaNovedadRepository meGustaNovedadRepository
    ) {
        this.meGustaNovedadRepository = meGustaNovedadRepository;
        this.novedadRepository = novedadRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.imagenRepository = imagenRepository;
        this.imagenService = imagenService;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
        this.notificacionService = notificacionService;
        this.feedEventService = feedEventService;
    }

    @Transactional
    public NovedadDTO publicar(Long userId, String texto, Long imagenId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        String textoLimpio = texto != null ? texto.trim() : "";
        if (textoLimpio.isEmpty()) {
            throw new FiltroInvalidoException("La novedad no puede estar vacia.");
        }
        if (textoLimpio.length() > MAX_TEXTO) {
            textoLimpio = textoLimpio.substring(0, MAX_TEXTO);
        }

        long publicadasHoy = novedadRepository
                .countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                        perfil.getId(),
                        inicioDelDiaArgentino()
                );

        if (publicadasHoy >= MAX_NOVEDADES_POR_DIA) {
            throw new FiltroInvalidoException(
                    "Podes publicar hasta " + MAX_NOVEDADES_POR_DIA
                            + " novedades por dia. Manana podes seguir contando."
            );
        }

        /* La foto tiene que ser suya y estar publicada. */
        Long imagenValidada = validarImagenPropia(perfil.getId(), imagenId);

        OffsetDateTime ahora = OffsetDateTime.now();

        Novedad novedad = new Novedad();
        novedad.setPerfilPublicadorId(perfil.getId());
        novedad.setTexto(textoLimpio);
        novedad.setImagenId(imagenValidada);
        novedad.setEstado(ESTADO_VISIBLE);
        novedad.setCreatedAt(ahora);
        novedad.setUpdatedAt(ahora);

        Novedad guardada = novedadRepository.saveAndFlush(novedad);

        /* Al feed de sus seguidores (best-effort, afterCommit). */
        feedEventService.emitirNovedad(
                perfil.getId(),
                guardada.getId(),
                imagenValidada,
                recortarParaResumen(textoLimpio)
        );

        /*
          Campanita SOLO en la primera del día: NotificacionService no
          agrupa ni deduplica, así que tres novedades por 50 seguidores
          serían 150 avisos a la misma gente en un día. Las demás igual
          entran al feed. Es la diferencia entre un canal que se sigue
          y uno que se silencia.
        */
        if (publicadasHoy == 0) {
            notificacionService.emitirATodos(
                    seguimientoPublicadorRepository.usuarioIdsSeguidoresDe(perfil.getId()),
                    "NOVEDAD_PUBLICADOR",
                    perfil.getNombre() + " publicó una novedad",
                    "/publicadores/" + (perfil.getSlug() != null
                            ? perfil.getSlug()
                            : perfil.getId())
            );
        }

        return enriquecer(List.of(guardada), perfil, userId).get(0);
    }

    /** Las visibles de un publicador (perfil público), paginadas. */
    @Transactional(readOnly = true)
    public List<NovedadDTO> listarPublicasDe(
            Long perfilPublicadorId,
            Long usuarioId,
            int limite
    ) {
        PerfilPublicador perfil = perfilPublicadorRepository
                .findByIdAndActivoTrue(perfilPublicadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El perfil publicador solicitado no existe o no está disponible."
                ));

        Page<Novedad> pagina = novedadRepository
                .findByPerfilPublicadorIdAndEstadoOrderByCreatedAtDesc(
                        perfilPublicadorId,
                        ESTADO_VISIBLE,
                        PageRequest.of(0, Math.min(Math.max(limite, 1), MAX_PAGINA))
                );

        return enriquecer(pagina.getContent(), perfil, usuarioId);
    }

    /** Las del publicador autenticado, para su panel. */
    @Transactional(readOnly = true)
    public List<NovedadDTO> listarMias(Long userId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        return enriquecer(
                novedadRepository.findByPerfilPublicadorIdAndEstadoNotOrderByCreatedAtDesc(
                        perfil.getId(),
                        ESTADO_ELIMINADA
                ),
                perfil,
                userId
        );
    }

    /* ===================== reacciones ===================== */

    /**
     * "Me gusta" en una novedad (script 37). Idempotente: el UNIQUE
     * hace que reaccionar dos veces no sume dos.
     *
     * NO notifica al publicador, y es deliberado: una novedad con
     * veinte reacciones serían veinte campanitas por algo que no pide
     * respuesta. Es el mismo criterio que los likes de fotos. La
     * campanita se reserva para lo que sí pide una acción.
     */
    @Transactional
    public long darMeGusta(Long usuarioId, Long novedadId) {
        validarUsuario(usuarioId);
        exigirVisible(novedadId);

        if (!meGustaNovedadRepository.existsByUsuarioIdAndNovedadId(usuarioId, novedadId)) {
            MeGustaNovedad meGusta = new MeGustaNovedad();
            meGusta.setUsuarioId(usuarioId);
            meGusta.setNovedadId(novedadId);
            meGusta.setCreatedAt(OffsetDateTime.now());

            try {
                meGustaNovedadRepository.saveAndFlush(meGusta);
            } catch (org.springframework.dao.DataIntegrityViolationException excepcion) {
                /* Otro request lo marcó en el medio: mismo resultado. */
            }
        }

        return meGustaNovedadRepository.countByNovedadId(novedadId);
    }

    @Transactional
    public long quitarMeGusta(Long usuarioId, Long novedadId) {
        validarUsuario(usuarioId);
        exigirVisible(novedadId);

        meGustaNovedadRepository
                .findByUsuarioIdAndNovedadId(usuarioId, novedadId)
                .ifPresent(meGustaNovedadRepository::delete);

        return meGustaNovedadRepository.countByNovedadId(novedadId);
    }

    /** Una novedad oculta o borrada no acepta reacciones. */
    private void exigirVisible(Long novedadId) {
        novedadRepository.findById(novedadId)
                .filter(novedad -> ESTADO_VISIBLE.equals(novedad.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La novedad solicitada no existe o no está disponible."
                ));
    }

    private void validarUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }

    /** El publicador borra la suya (baja lógica). */
    @Transactional
    public void eliminarPropia(Long userId, Long novedadId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        Novedad novedad = novedadRepository.findById(novedadId)
                /* 404 y no 403: no delatamos novedades ajenas. */
                .filter(encontrada -> encontrada.getPerfilPublicadorId().equals(perfil.getId()))
                .filter(encontrada -> !ESTADO_ELIMINADA.equals(encontrada.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la novedad."));

        novedad.setEstado(ESTADO_ELIMINADA);
        novedad.setUpdatedAt(OffsetDateTime.now());
        novedadRepository.save(novedad);
    }

    /** El admin la oculta (moderación reactiva). */
    @Transactional
    public void ocultarPorAdmin(Long novedadId) {
        Novedad novedad = novedadRepository.findById(novedadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la novedad."));

        novedad.setEstado(ESTADO_OCULTA);
        novedad.setUpdatedAt(OffsetDateTime.now());
        novedadRepository.save(novedad);
    }

    /** ¿Visible? (para reportarla). */
    @Transactional(readOnly = true)
    public boolean esVisible(Long novedadId) {
        return novedadRepository.findById(novedadId)
                .filter(novedad -> ESTADO_VISIBLE.equals(novedad.getEstado()))
                .isPresent();
    }

    /* ===================== interno ===================== */

    private List<NovedadDTO> enriquecer(
            List<Novedad> novedades,
            PerfilPublicador perfil,
            Long usuarioId
    ) {
        if (novedades.isEmpty()) {
            return List.of();
        }

        List<Long> imagenIds = novedades.stream()
                .map(Novedad::getImagenId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Imagen> imagenes = imagenIds.isEmpty()
                ? Map.of()
                : imagenRepository.findAllById(imagenIds).stream()
                        .collect(java.util.HashMap::new,
                                (mapa, imagen) -> mapa.put(imagen.getId(), imagen),
                                java.util.HashMap::putAll);

        /* Reacciones (script 37), en batch: nunca una query por novedad. */
        List<Long> novedadIds = novedades.stream()
                .map(Novedad::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, Long> meGustaPorNovedad = new java.util.HashMap<>();
        List<Long> conMeGustaPropio = List.of();

        if (!novedadIds.isEmpty()) {
            for (MeGustaNovedadRepository.ConteoMeGusta conteo
                    : meGustaNovedadRepository.contarPorNovedades(novedadIds)) {
                meGustaPorNovedad.put(conteo.getNovedadId(), conteo.getCantidad());
            }

            if (usuarioId != null) {
                conMeGustaPropio =
                        meGustaNovedadRepository.novedadIdsConMeGustaDe(usuarioId, novedadIds);
            }
        }

        final List<Long> propias = conMeGustaPropio;

        String logoUrl = imagenService
                .obtenerLogosAprobadosPorPerfil(List.of(perfil.getId()))
                .get(perfil.getId());

        return novedades.stream()
                .map(novedad -> {
                    NovedadDTO dto = new NovedadDTO();
                    dto.setId(novedad.getId());
                    dto.setTexto(novedad.getTexto());
                    dto.setCreatedAt(novedad.getCreatedAt());
                    dto.setPerfilPublicadorId(perfil.getId());
                    dto.setPerfilNombre(perfil.getNombre());
                    dto.setPerfilSlug(perfil.getSlug());
                    dto.setPerfilLogoUrl(logoUrl);

                    Imagen imagen = novedad.getImagenId() != null
                            ? imagenes.get(novedad.getImagenId())
                            : null;

                    if (imagen != null && ImagenMapper.esUrlPublicable(imagen.getUrl())) {
                        dto.setImagenId(imagen.getId());
                        dto.setImagenUrl(imagen.getUrl());
                    }

                    /*
                      El id se chequea antes de buscar: las listas
                      inmutables de Java lanzan NPE con `contains(null)`,
                      y una novedad sin id sería un NPE en el enriquecido
                      entero por un campo decorativo.
                    */
                    dto.setCantidadMeGusta(novedad.getId() != null
                            ? meGustaPorNovedad.getOrDefault(novedad.getId(), 0L)
                            : 0L);
                    dto.setMeGusta(novedad.getId() != null
                            && propias.contains(novedad.getId()));

                    return dto;
                })
                .toList();
    }

    /**
     * La foto tiene que ser del publicador y estar publicada. Si manda
     * una ajena o inexistente, la novedad se publica SIN foto en vez
     * de fallar: el texto es lo que importa.
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

    private String recortarParaResumen(String texto) {
        return texto.length() <= 120 ? texto : texto.substring(0, 120) + "...";
    }

    private OffsetDateTime inicioDelDiaArgentino() {
        return LocalDate.now(ZONA_ARGENTINA)
                .atStartOfDay(ZONA_ARGENTINA)
                .toOffsetDateTime();
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
