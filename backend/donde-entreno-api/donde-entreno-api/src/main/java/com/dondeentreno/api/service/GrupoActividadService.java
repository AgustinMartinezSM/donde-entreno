package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.AvisoGrupoDTO;
import com.dondeentreno.api.dto.ComentarioAvisoDTO;
import com.dondeentreno.api.dto.GrupoActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.AvisoGrupo;
import com.dondeentreno.api.entity.ComentarioAviso;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.MeGustaAviso;
import com.dondeentreno.api.entity.MiembroActividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.mapper.ImagenMapper;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.AvisoGrupoRepository;
import com.dondeentreno.api.repository.ComentarioAvisoRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaAvisoRepository;
import com.dondeentreno.api.repository.MiembroActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Grupos por actividad (script 38).
 *
 * El grupo es el espacio de una actividad PARA QUIENES VAN. Reglas que
 * viven acá y no en el schema:
 *
 * - **Se entra explícitamente.** El check-in ("entrené acá") NO da
 *   pertenencia: marcar que entrenaste una vez es un acto distinto de
 *   sumarte a un espacio donde vas a recibir avisos y donde otros ven
 *   lo que escribís.
 * - **Escribe el publicador; los miembros comentan y reaccionan.** No
 *   hay chat libre entre miembros: es el canal más difícil de moderar
 *   del producto y se decide con grupos vivos (V2 del roadmap).
 * - **El grupo es privado de sus miembros**, y no existe ningún método
 *   que devuelva su contenido a un admin. Ante un reporte, el admin
 *   actúa sobre el objeto reportado, no lee el grupo.
 * - **Solo actividades PUBLICADAS tienen grupo.**
 */
@Service
public class GrupoActividadService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private static final String AVISO_VISIBLE = "VISIBLE";
    private static final String AVISO_OCULTO_ADMIN = "OCULTO_POR_ADMIN";
    private static final String AVISO_ELIMINADO = "ELIMINADO_POR_PUBLICADOR";

    private static final String COMENTARIO_VISIBLE = "VISIBLE";
    private static final String COMENTARIO_OCULTO_PUBLICADOR = "OCULTO_POR_PUBLICADOR";
    private static final String COMENTARIO_OCULTO_ADMIN = "OCULTO_POR_ADMIN";
    private static final String COMENTARIO_ELIMINADO = "ELIMINADO_POR_AUTOR";

    /**
     * Tope de avisos por día y por actividad. Más bajo que el de
     * novedades (3) porque el grupo es más íntimo: acá el aviso llega
     * a gente que ya eligió estar, y saturarlos es la forma más rápida
     * de que se vayan.
     */
    private static final int MAX_AVISOS_POR_DIA = 2;

    /** Mismo tope que los comentarios de fotos. */
    private static final int MAX_COMENTARIOS_POR_DIA = 20;

    private static final int MAX_TEXTO_AVISO = 1000;
    private static final int MAX_TEXTO_COMENTARIO = 500;
    private static final int MAX_AVISOS_LISTADOS = 30;

    private final MiembroActividadRepository miembroRepository;
    private final AvisoGrupoRepository avisoRepository;
    private final ComentarioAvisoRepository comentarioRepository;
    private final MeGustaAvisoRepository meGustaRepository;
    private final ActividadRepository actividadRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ImagenRepository imagenRepository;
    private final NotificacionService notificacionService;

    public GrupoActividadService(
            MiembroActividadRepository miembroRepository,
            AvisoGrupoRepository avisoRepository,
            ComentarioAvisoRepository comentarioRepository,
            MeGustaAvisoRepository meGustaRepository,
            ActividadRepository actividadRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            UsuarioRepository usuarioRepository,
            ImagenRepository imagenRepository,
            NotificacionService notificacionService
    ) {
        this.miembroRepository = miembroRepository;
        this.avisoRepository = avisoRepository;
        this.comentarioRepository = comentarioRepository;
        this.meGustaRepository = meGustaRepository;
        this.actividadRepository = actividadRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.imagenRepository = imagenRepository;
        this.notificacionService = notificacionService;
    }

    /* ===================== pertenencia ===================== */

    @Transactional
    public GrupoActividadDTO unirse(Long usuarioId, Long actividadId) {
        validarUsuario(usuarioId);
        Actividad actividad = exigirActividadPublicada(actividadId);

        OffsetDateTime ahora = OffsetDateTime.now();

        MiembroActividad miembro = miembroRepository
                .findByUsuarioIdAndActividadId(usuarioId, actividadId)
                .orElseGet(() -> {
                    MiembroActividad nuevo = new MiembroActividad();
                    nuevo.setUsuarioId(usuarioId);
                    nuevo.setActividadId(actividadId);
                    nuevo.setCreatedAt(ahora);
                    return nuevo;
                });

        /* Volver a entrar reusa la fila: no se pierde la fecha original. */
        miembro.setEstado(MiembroActividad.ESTADO_ACTIVO);
        miembro.setUpdatedAt(ahora);
        miembroRepository.saveAndFlush(miembro);

        return verGrupo(usuarioId, actividad);
    }

    @Transactional
    public void salir(Long usuarioId, Long actividadId) {
        validarUsuario(usuarioId);

        miembroRepository.findByUsuarioIdAndActividadId(usuarioId, actividadId)
                .ifPresent(miembro -> {
                    miembro.setEstado(MiembroActividad.ESTADO_SALIO);
                    miembro.setUpdatedAt(OffsetDateTime.now());
                    miembroRepository.save(miembro);
                });
    }

    /* ===================== lectura ===================== */

    /**
     * El grupo. Si no es miembro devuelve la ficha SIN contenido:
     * `esMiembro=false` y `avisos` vacío. Así el frontend puede
     * ofrecer "Sumarme al grupo" sin tener que filtrar nada, y el
     * contenido no sale del backend para quien no corresponde.
     */
    @Transactional(readOnly = true)
    public GrupoActividadDTO verGrupo(Long usuarioId, Long actividadId) {
        return verGrupo(usuarioId, exigirActividadPublicada(actividadId));
    }

    private GrupoActividadDTO verGrupo(Long usuarioId, Actividad actividad) {
        boolean esMiembro = usuarioId != null
                && miembroRepository.existsByUsuarioIdAndActividadIdAndEstado(
                        usuarioId, actividad.getId(), MiembroActividad.ESTADO_ACTIVO);

        GrupoActividadDTO dto = new GrupoActividadDTO();
        dto.setActividadId(actividad.getId());
        dto.setActividadTitulo(actividad.getTitulo());
        dto.setActividadSlug(actividad.getSlug());
        /* `esMiembro` refleja la membresía REAL: es lo que decide el botón. */
        dto.setEsMiembro(esMiembro);
        dto.setCantidadMiembros(miembroRepository.countByActividadIdAndEstado(
                actividad.getId(), MiembroActividad.ESTADO_ACTIVO));

        /*
          El dueño de la actividad ve su grupo SIN ser miembro: es su
          espacio, y es quien lo modera. Sin esto su propio panel le
          devolvería una lista vacía —el publicador no "va" a su
          actividad, así que nunca se suma—.
        */
        dto.setAvisos(esMiembro || esDuenioDeLaActividad(usuarioId, actividad)
                ? listarAvisos(actividad.getId(), usuarioId)
                : List.of());

        return dto;
    }

    /** El dueño de la actividad, para acceso de administración. */
    private boolean esDuenioDeLaActividad(Long usuarioId, Actividad actividad) {
        return usuarioId != null
                && actividad.getPerfilPublicador() != null
                && actividad.getPerfilPublicador().getUsuario() != null
                && usuarioId.equals(actividad.getPerfilPublicador().getUsuario().getId());
    }

    /** Un aviso con sus comentarios. Solo para miembros. */
    @Transactional(readOnly = true)
    public AvisoGrupoDTO verAviso(Long usuarioId, Long avisoId) {
        AvisoGrupo aviso = exigirAvisoVisibleParaMiembro(usuarioId, avisoId);

        AvisoGrupoDTO dto = armarAvisos(List.of(aviso), usuarioId).get(0);
        dto.setComentarios(
                comentarioRepository.findByAvisoIdOrderByCreatedAtAsc(avisoId).stream()
                        .filter(comentario ->
                                COMENTARIO_VISIBLE.equals(comentario.getEstado()))
                        .map(comentario -> toComentarioDTO(comentario, usuarioId))
                        .toList()
        );

        return dto;
    }

    /* ===================== publicador ===================== */

    @Transactional
    public AvisoGrupoDTO avisar(Long userId, Long actividadId, String texto, Long imagenId) {
        PerfilPublicador perfil = obtenerPerfil(userId);
        Actividad actividad = exigirActividadPublicada(actividadId);

        if (actividad.getPerfilPublicador() == null
                || !perfil.getId().equals(actividad.getPerfilPublicador().getId())) {
            throw new RecursoNoEncontradoException("No se encontro la actividad.");
        }

        String limpio = exigirTexto(texto, MAX_TEXTO_AVISO);

        if (avisoRepository.countByActividadIdAndCreatedAtGreaterThanEqual(
                actividadId, inicioDelDiaArgentino()) >= MAX_AVISOS_POR_DIA) {
            throw new FiltroInvalidoException(
                    "Podes avisar hasta " + MAX_AVISOS_POR_DIA
                            + " veces por dia en este grupo. Manana podes seguir."
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        AvisoGrupo aviso = new AvisoGrupo();
        aviso.setActividadId(actividadId);
        aviso.setTexto(limpio);
        aviso.setImagenId(validarImagenPropia(perfil.getId(), imagenId));
        aviso.setEstado(AVISO_VISIBLE);
        aviso.setCreatedAt(ahora);
        aviso.setUpdatedAt(ahora);

        AvisoGrupo guardado = avisoRepository.saveAndFlush(aviso);

        /*
          El aviso SÍ notifica: es la razón de ser del grupo. Los
          comentarios de los miembros, en cambio, no notifican a todos
          (ver `comentar`).
        */
        notificacionService.emitirATodos(
                miembroRepository.usuarioIdsActivosDe(actividadId),
                "AVISO_GRUPO",
                perfil.getNombre() + " avisó algo en " + actividad.getTitulo(),
                "/actividades/" + actividad.getSlug() + "?tab=grupo"
        );

        return armarAvisos(List.of(guardado), userId).get(0);
    }

    @Transactional
    public void eliminarAviso(Long userId, Long avisoId) {
        PerfilPublicador perfil = obtenerPerfil(userId);
        AvisoGrupo aviso = exigirAvisoDelPublicador(perfil.getId(), avisoId);

        aviso.setEstado(AVISO_ELIMINADO);
        aviso.setUpdatedAt(OffsetDateTime.now());
        avisoRepository.save(aviso);
    }

    /** El publicador modera su propio grupo (patrón de la Fase 4). */
    @Transactional
    public void ocultarComentarioPorPublicador(Long userId, Long comentarioId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        ComentarioAviso comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el comentario."));

        /* Tiene que ser un comentario de un aviso de SU actividad. */
        exigirAvisoDelPublicador(perfil.getId(), comentario.getAvisoId());

        comentario.setEstado(COMENTARIO_OCULTO_PUBLICADOR);
        comentarioRepository.save(comentario);
    }

    /* ===================== miembros ===================== */

    @Transactional
    public ComentarioAvisoDTO comentar(Long usuarioId, Long avisoId, String texto) {
        AvisoGrupo aviso = exigirAvisoVisibleParaMiembro(usuarioId, avisoId);
        String limpio = exigirTexto(texto, MAX_TEXTO_COMENTARIO);

        if (comentarioRepository.countByUsuarioIdAndCreatedAtGreaterThanEqual(
                usuarioId, inicioDelDiaArgentino()) >= MAX_COMENTARIOS_POR_DIA) {
            throw new FiltroInvalidoException(
                    "Llegaste al maximo de comentarios por hoy."
            );
        }

        ComentarioAviso comentario = new ComentarioAviso();
        comentario.setAvisoId(avisoId);
        comentario.setUsuarioId(usuarioId);
        comentario.setTexto(limpio);
        comentario.setEstado(COMENTARIO_VISIBLE);
        comentario.setCreatedAt(OffsetDateTime.now());

        ComentarioAviso guardado = comentarioRepository.saveAndFlush(comentario);

        /*
          Se avisa SOLO al publicador, no a los demás miembros: un
          aviso con quince comentarios serían quince campanitas para
          cada uno de ellos.
        */
        actividadRepository.findById(aviso.getActividadId())
                .filter(actividad -> actividad.getPerfilPublicador() != null
                        && actividad.getPerfilPublicador().getUsuario() != null)
                .ifPresent(actividad -> notificacionService.emitir(
                        actividad.getPerfilPublicador().getUsuario().getId(),
                        "COMENTARIO_GRUPO",
                        "Comentaron en tu aviso de " + actividad.getTitulo(),
                        "/publicador/actividades/" + actividad.getId() + "/grupo"
                ));

        return toComentarioDTO(guardado, usuarioId);
    }

    @Transactional
    public void eliminarComentarioPropio(Long usuarioId, Long comentarioId) {
        validarUsuario(usuarioId);

        ComentarioAviso comentario = comentarioRepository.findById(comentarioId)
                /* 404 y no 403: no se delata que existe. */
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el comentario."));

        comentario.setEstado(COMENTARIO_ELIMINADO);
        comentarioRepository.save(comentario);
    }

    @Transactional
    public long darMeGusta(Long usuarioId, Long avisoId) {
        exigirAvisoVisibleParaMiembro(usuarioId, avisoId);

        if (!meGustaRepository.existsByUsuarioIdAndAvisoId(usuarioId, avisoId)) {
            MeGustaAviso meGusta = new MeGustaAviso();
            meGusta.setUsuarioId(usuarioId);
            meGusta.setAvisoId(avisoId);
            meGusta.setCreatedAt(OffsetDateTime.now());

            try {
                meGustaRepository.saveAndFlush(meGusta);
            } catch (org.springframework.dao.DataIntegrityViolationException excepcion) {
                /* Otro request lo marcó en el medio: mismo resultado. */
            }
        }

        return meGustaRepository.countByAvisoId(avisoId);
    }

    @Transactional
    public long quitarMeGusta(Long usuarioId, Long avisoId) {
        exigirAvisoVisibleParaMiembro(usuarioId, avisoId);

        meGustaRepository.findByUsuarioIdAndAvisoId(usuarioId, avisoId)
                .ifPresent(meGustaRepository::delete);

        return meGustaRepository.countByAvisoId(avisoId);
    }

    /* ===================== moderación admin ===================== */

    @Transactional(readOnly = true)
    public boolean esVisibleAviso(Long avisoId) {
        return avisoRepository.findById(avisoId)
                .filter(aviso -> AVISO_VISIBLE.equals(aviso.getEstado()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean esVisibleComentario(Long comentarioId) {
        return comentarioRepository.findById(comentarioId)
                .filter(comentario -> COMENTARIO_VISIBLE.equals(comentario.getEstado()))
                .isPresent();
    }

    @Transactional
    public void ocultarAvisoPorAdmin(Long avisoId) {
        AvisoGrupo aviso = avisoRepository.findById(avisoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el aviso."));

        aviso.setEstado(AVISO_OCULTO_ADMIN);
        aviso.setUpdatedAt(OffsetDateTime.now());
        avisoRepository.save(aviso);
    }

    @Transactional
    public void ocultarComentarioPorAdmin(Long comentarioId) {
        ComentarioAviso comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el comentario."));

        comentario.setEstado(COMENTARIO_OCULTO_ADMIN);
        comentarioRepository.save(comentario);
    }

    /* ===================== interno ===================== */

    private List<AvisoGrupoDTO> listarAvisos(Long actividadId, Long usuarioId) {
        return armarAvisos(
                avisoRepository.findByActividadIdAndEstadoOrderByCreatedAtDesc(
                        actividadId,
                        AVISO_VISIBLE,
                        PageRequest.of(0, MAX_AVISOS_LISTADOS)
                ).getContent(),
                usuarioId
        );
    }

    /** Conteos y foto en queries batch: nunca una por aviso. */
    private List<AvisoGrupoDTO> armarAvisos(List<AvisoGrupo> avisos, Long usuarioId) {
        if (avisos.isEmpty()) {
            return List.of();
        }

        List<Long> avisoIds = avisos.stream()
                .map(AvisoGrupo::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Long> meGustaPorAviso = new HashMap<>();
        Map<Long, Long> comentariosPorAviso = new HashMap<>();
        List<Long> propios = List.of();

        if (!avisoIds.isEmpty()) {
            for (MeGustaAvisoRepository.ConteoMeGustaAviso conteo
                    : meGustaRepository.contarPorAvisos(avisoIds)) {
                meGustaPorAviso.put(conteo.getAvisoId(), conteo.getCantidad());
            }

            for (ComentarioAvisoRepository.ConteoComentarios conteo
                    : comentarioRepository.contarPorAvisos(avisoIds)) {
                comentariosPorAviso.put(conteo.getAvisoId(), conteo.getCantidad());
            }

            if (usuarioId != null) {
                propios = meGustaRepository.avisoIdsConMeGustaDe(usuarioId, avisoIds);
            }
        }

        List<Long> imagenIds = avisos.stream()
                .map(AvisoGrupo::getImagenId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Imagen> imagenes = imagenIds.isEmpty()
                ? Map.of()
                : imagenRepository.findAllById(imagenIds).stream()
                        .collect(HashMap::new,
                                (mapa, imagen) -> mapa.put(imagen.getId(), imagen),
                                HashMap::putAll);

        final List<Long> propiosFinal = propios;

        return avisos.stream().map(aviso -> {
            AvisoGrupoDTO dto = new AvisoGrupoDTO();
            dto.setId(aviso.getId());
            dto.setTexto(aviso.getTexto());
            dto.setCreatedAt(aviso.getCreatedAt());

            Imagen imagen = aviso.getImagenId() != null
                    ? imagenes.get(aviso.getImagenId())
                    : null;
            if (imagen != null && ImagenMapper.esUrlPublicable(imagen.getUrl())) {
                dto.setImagenId(imagen.getId());
                dto.setImagenUrl(imagen.getUrl());
            }

            /* El id se chequea antes: `List.of().contains(null)` es NPE. */
            dto.setCantidadMeGusta(aviso.getId() != null
                    ? meGustaPorAviso.getOrDefault(aviso.getId(), 0L)
                    : 0L);
            dto.setMeGusta(aviso.getId() != null && propiosFinal.contains(aviso.getId()));
            dto.setCantidadComentarios(aviso.getId() != null
                    ? comentariosPorAviso.getOrDefault(aviso.getId(), 0L)
                    : 0L);

            return dto;
        }).toList();
    }

    private ComentarioAvisoDTO toComentarioDTO(ComentarioAviso comentario, Long usuarioId) {
        ComentarioAvisoDTO dto = new ComentarioAvisoDTO();
        dto.setId(comentario.getId());
        dto.setTexto(comentario.getTexto());
        dto.setCreatedAt(comentario.getCreatedAt());
        dto.setEsPropio(comentario.getUsuarioId().equals(usuarioId));
        dto.setAutorNombre(nombreCorto(comentario.getUsuarioId()));
        return dto;
    }

    /**
     * El aviso, exigiendo que quien lo pide sea MIEMBRO ACTIVO.
     *
     * 404 y no 403 para el no-miembro: el grupo es privado, así que ni
     * siquiera se confirma que ese aviso exista.
     */
    private AvisoGrupo exigirAvisoVisibleParaMiembro(Long usuarioId, Long avisoId) {
        validarUsuario(usuarioId);

        AvisoGrupo aviso = avisoRepository.findById(avisoId)
                .filter(encontrado -> AVISO_VISIBLE.equals(encontrado.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el aviso."));

        boolean esMiembro = miembroRepository.existsByUsuarioIdAndActividadIdAndEstado(
                usuarioId, aviso.getActividadId(), MiembroActividad.ESTADO_ACTIVO);

        /* El dueño también entra: es quien modera los comentarios. */
        boolean esDuenio = actividadRepository.findById(aviso.getActividadId())
                .map(actividad -> esDuenioDeLaActividad(usuarioId, actividad))
                .orElse(false);

        if (!esMiembro && !esDuenio) {
            throw new RecursoNoEncontradoException("No se encontro el aviso.");
        }

        return aviso;
    }

    private AvisoGrupo exigirAvisoDelPublicador(Long perfilId, Long avisoId) {
        AvisoGrupo aviso = avisoRepository.findById(avisoId)
                .filter(encontrado -> !AVISO_ELIMINADO.equals(encontrado.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el aviso."));

        Actividad actividad = actividadRepository.findById(aviso.getActividadId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el aviso."));

        if (actividad.getPerfilPublicador() == null
                || !perfilId.equals(actividad.getPerfilPublicador().getId())) {
            throw new RecursoNoEncontradoException("No se encontro el aviso.");
        }

        return aviso;
    }

    /** Solo actividades PUBLICADAS tienen grupo. */
    private Actividad exigirActividadPublicada(Long actividadId) {
        return actividadRepository.findById(actividadId)
                .filter(actividad -> Boolean.TRUE.equals(actividad.getActiva())
                        && "PUBLICADA".equals(actividad.getEstadoPublicacion())
                        && actividad.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La actividad solicitada no existe o no está disponible."
                ));
    }

    private Long validarImagenPropia(Long perfilId, Long imagenId) {
        if (imagenId == null) {
            return null;
        }

        return imagenRepository.findById(imagenId)
                .filter(imagen -> Boolean.TRUE.equals(imagen.getActiva()))
                .filter(imagen -> "APROBADA".equals(imagen.getEstadoModeracion()))
                .filter(imagen -> esDelPublicador(imagen, perfilId))
                .map(Imagen::getId)
                /* Foto ajena: el aviso sale sin foto, el texto importa más. */
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

    private String nombreCorto(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(usuario -> {
                    String nombre = usuario.getNombre() != null
                            ? usuario.getNombre().trim() : "";
                    String apellido = usuario.getApellido() != null
                            ? usuario.getApellido().trim() : "";

                    if (nombre.isEmpty()) {
                        return "Alguien de la comunidad";
                    }

                    return apellido.isEmpty()
                            ? nombre
                            : nombre + " "
                                    + apellido.substring(0, 1).toUpperCase(Locale.ROOT) + ".";
                })
                .orElse("Alguien de la comunidad");
    }

    private PerfilPublicador obtenerPerfil(Long userId) {
        validarUsuario(userId);

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Perfil publicador no encontrado."
                ));
    }

    private String exigirTexto(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            throw new FiltroInvalidoException("El texto no puede estar vacio.");
        }

        String limpio = texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    private void validarUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }

    private OffsetDateTime inicioDelDiaArgentino() {
        return LocalDate.now(ZONA_ARGENTINA)
                .atStartOfDay(ZONA_ARGENTINA)
                .toOffsetDateTime();
    }
}
