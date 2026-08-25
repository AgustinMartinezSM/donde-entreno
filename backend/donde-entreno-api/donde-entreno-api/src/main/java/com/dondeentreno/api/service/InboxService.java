package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ConversacionDTO;
import com.dondeentreno.api.dto.MensajeDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Conversacion;
import com.dondeentreno.api.entity.Mensaje;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ConversacionRepository;
import com.dondeentreno.api.repository.MensajeRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
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
 * Inbox de consultas usuario ↔ publicador (script 36).
 *
 * Reglas de producto que viven acá y no en el schema:
 *
 * - **La conversación la inicia SIEMPRE el usuario.** El publicador no
 *   puede escribir en frío: un primer mensaje no solicitado destruye
 *   la confianza en la bandeja entera.
 * - **Solo el usuario puede cerrar**, y cerrada, el publicador no
 *   puede seguir escribiendo.
 * - **El admin NO lee conversaciones.** La única puerta es un mensaje
 *   REPORTADO, y ve ese mensaje con contexto mínimo
 *   (`contextoDeReporte`). No existe ningún método que devuelva un
 *   hilo completo a un admin.
 */
@Service
public class InboxService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    public static final String ESTADO_ABIERTA = "ABIERTA";
    public static final String ESTADO_CERRADA = "CERRADA_POR_USUARIO";
    private static final String MENSAJE_VISIBLE = "VISIBLE";
    private static final String MENSAJE_OCULTO = "OCULTO_POR_ADMIN";

    /**
     * Topes del usuario. Texto libre, privado y sin fricción es
     * exactamente donde entra el spam. El PUBLICADOR no tiene tope
     * para responder: responder es lo que queremos que pase.
     */
    private static final int MAX_CONVERSACIONES_POR_DIA = 5;
    private static final int MAX_MENSAJES_POR_DIA = 20;

    private static final int MAX_TEXTO = 2000;
    private static final int LARGO_VISTA_PREVIA = 120;

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ImagenService imagenService;
    private final NotificacionService notificacionService;

    public InboxService(
            ConversacionRepository conversacionRepository,
            MensajeRepository mensajeRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            UsuarioRepository usuarioRepository,
            ImagenService imagenService,
            NotificacionService notificacionService
    ) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.usuarioRepository = usuarioRepository;
        this.imagenService = imagenService;
        this.notificacionService = notificacionService;
    }

    /* ===================== usuario ===================== */

    /**
     * El usuario consulta. Si ya había un hilo con ese publicador por
     * esa actividad, escribe ahí en vez de abrir uno nuevo.
     */
    @Transactional
    public ConversacionDTO consultar(
            Long usuarioId,
            Long perfilPublicadorId,
            Long actividadId,
            String texto
    ) {
        validarUsuario(usuarioId);
        String mensaje = exigirTexto(texto);

        PerfilPublicador perfil = perfilPublicadorRepository
                .findByIdAndActivoTrue(perfilPublicadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El perfil publicador solicitado no existe o no está disponible."
                ));

        /* El publicador no se consulta a sí mismo. */
        if (perfil.getUsuario() != null && usuarioId.equals(perfil.getUsuario().getId())) {
            throw new FiltroInvalidoException("No podés consultarte a vos mismo.");
        }

        Long actividadValidada = validarActividadDelPerfil(perfilPublicadorId, actividadId);

        if (mensajeRepository.contarDelUsuarioDesde(usuarioId, inicioDelDiaArgentino())
                >= MAX_MENSAJES_POR_DIA) {
            throw new FiltroInvalidoException(
                    "Llegaste al máximo de mensajes por hoy. Mañana podés seguir."
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        Conversacion conversacion = conversacionRepository
                .buscarExistente(usuarioId, perfilPublicadorId, actividadValidada)
                .orElse(null);

        if (conversacion == null) {
            if (conversacionRepository.countByUsuarioIdAndCreatedAtGreaterThanEqual(
                    usuarioId, inicioDelDiaArgentino()) >= MAX_CONVERSACIONES_POR_DIA) {
                throw new FiltroInvalidoException(
                        "Abriste muchas consultas hoy. Mañana podés seguir."
                );
            }

            conversacion = new Conversacion();
            conversacion.setUsuarioId(usuarioId);
            conversacion.setPerfilPublicadorId(perfilPublicadorId);
            conversacion.setActividadId(actividadValidada);
            conversacion.setEstado(ESTADO_ABIERTA);
            conversacion.setUltimoMensajeAt(ahora);
            conversacion.setCreatedAt(ahora);
            conversacion.setUpdatedAt(ahora);
        } else {
            /* Volver a escribir reabre lo que el propio usuario cerró. */
            conversacion.setEstado(ESTADO_ABIERTA);
            conversacion.setUltimoMensajeAt(ahora);
            conversacion.setUpdatedAt(ahora);
        }

        Conversacion guardada = conversacionRepository.saveAndFlush(conversacion);

        agregarMensaje(guardada.getId(), Mensaje.AUTOR_USUARIO, mensaje, ahora);

        /* Al dueño del perfil, que es quien responde. */
        if (perfil.getUsuario() != null) {
            notificacionService.emitir(
                    perfil.getUsuario().getId(),
                    "MENSAJE_NUEVO",
                    "Tenés una consulta nueva",
                    "/publicador/consultas"
            );
        }

        return armar(List.of(guardada), Mensaje.AUTOR_USUARIO).get(0);
    }

    @Transactional(readOnly = true)
    public List<ConversacionDTO> bandejaDelUsuario(Long usuarioId) {
        validarUsuario(usuarioId);

        return armar(
                conversacionRepository.findByUsuarioIdOrderByUltimoMensajeAtDesc(usuarioId),
                Mensaje.AUTOR_USUARIO
        );
    }

    /** El número del badge de "Mis consultas". */
    @Transactional(readOnly = true)
    public long contarNoLeidosDelUsuario(Long usuarioId) {
        validarUsuario(usuarioId);

        return mensajeRepository.contarNoLeidosDelUsuario(usuarioId);
    }

    /** Ídem del lado del publicador. */
    @Transactional(readOnly = true)
    public long contarNoLeidosDelPublicador(Long userId) {
        return mensajeRepository.contarNoLeidosDelPublicador(obtenerPerfil(userId).getId());
    }

    /** Cerrar es del usuario: deja de recibir y el otro no puede escribir. */
    @Transactional
    public void cerrar(Long usuarioId, Long conversacionId) {
        Conversacion conversacion = exigirDelUsuario(usuarioId, conversacionId);

        conversacion.setEstado(ESTADO_CERRADA);
        conversacion.setUpdatedAt(OffsetDateTime.now());
        conversacionRepository.save(conversacion);
    }

    /* ===================== publicador ===================== */

    @Transactional(readOnly = true)
    public List<ConversacionDTO> bandejaDelPublicador(Long userId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        return armar(
                conversacionRepository.findByPerfilPublicadorIdOrderByUltimoMensajeAtDesc(
                        perfil.getId()
                ),
                Mensaje.AUTOR_PUBLICADOR
        );
    }

    @Transactional
    public ConversacionDTO responder(Long userId, Long conversacionId, String texto) {
        PerfilPublicador perfil = obtenerPerfil(userId);
        String mensaje = exigirTexto(texto);

        Conversacion conversacion = conversacionRepository.findById(conversacionId)
                .filter(cada -> cada.getPerfilPublicadorId().equals(perfil.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la conversacion."
                ));

        /*
          Cerrada por el usuario = no se puede seguir escribiendo. Es la
          mitad que hace que "cerrar" signifique algo.
        */
        if (ESTADO_CERRADA.equals(conversacion.getEstado())) {
            throw new FiltroInvalidoException(
                    "La persona cerró esta consulta, no podés responderla."
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        agregarMensaje(conversacion.getId(), Mensaje.AUTOR_PUBLICADOR, mensaje, ahora);

        conversacion.setUltimoMensajeAt(ahora);
        conversacion.setUpdatedAt(ahora);
        Conversacion guardada = conversacionRepository.saveAndFlush(conversacion);

        notificacionService.emitir(
                conversacion.getUsuarioId(),
                "MENSAJE_NUEVO",
                perfil.getNombre() + " respondió tu consulta",
                "/mi-cuenta/consultas"
        );

        return armar(List.of(guardada), Mensaje.AUTOR_PUBLICADOR).get(0);
    }

    /* ===================== hilo ===================== */

    /**
     * El hilo, para cualquiera de los dos lados. Marca leído lo que
     * escribió el otro: abrir la conversación ES haberla leído.
     */
    @Transactional
    public ConversacionDTO verHilo(Long userId, Long conversacionId, boolean comoPublicador) {
        Conversacion conversacion = comoPublicador
                ? exigirDelPublicador(userId, conversacionId)
                : exigirDelUsuario(userId, conversacionId);

        String autorPropio = comoPublicador
                ? Mensaje.AUTOR_PUBLICADOR
                : Mensaje.AUTOR_USUARIO;

        mensajeRepository.marcarLeidos(
                conversacion.getId(), autorPropio, OffsetDateTime.now());

        ConversacionDTO dto = armar(List.of(conversacion), autorPropio).get(0);
        dto.setNoLeidos(0L);
        dto.setMensajes(
                mensajeRepository
                        .findByConversacionIdOrderByCreatedAtAsc(conversacion.getId())
                        .stream()
                        .map(mensaje -> toMensajeDTO(mensaje, autorPropio))
                        .toList()
        );

        return dto;
    }

    /* ===================== moderación ===================== */

    /** ¿Visible? (para reportarlo). */
    @Transactional(readOnly = true)
    public boolean esVisibleMensaje(Long mensajeId) {
        return mensajeRepository.findById(mensajeId)
                .filter(mensaje -> MENSAJE_VISIBLE.equals(mensaje.getEstado()))
                .isPresent();
    }

    @Transactional
    public void ocultarMensajePorAdmin(Long mensajeId) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el mensaje."));

        mensaje.setEstado(MENSAJE_OCULTO);
        mensajeRepository.save(mensaje);
    }

    /**
     * Lo ÚNICO que el admin puede ver de una conversación privada: el
     * mensaje reportado y, a lo sumo, los dos anteriores.
     *
     * El contexto existe porque sin él no se puede juzgar un mensaje
     * (una respuesta agresiva puede ser a una provocación), y se corta
     * en dos porque más que eso ya es leer la conversación de otro.
     * NO hay ningún otro método que devuelva mensajes a un admin.
     */
    @Transactional(readOnly = true)
    public List<MensajeDTO> contextoDeReporte(Long mensajeId) {
        Mensaje reportado = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el mensaje."));

        return mensajeRepository
                .findTop3ByConversacionIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                        reportado.getConversacionId(),
                        reportado.getCreatedAt()
                )
                .stream()
                /* Del más viejo al más nuevo: se lee como conversación. */
                .sorted((uno, otro) -> uno.getCreatedAt().compareTo(otro.getCreatedAt()))
                .map(mensaje -> {
                    MensajeDTO dto = new MensajeDTO();
                    dto.setId(mensaje.getId());
                    dto.setTexto(mensaje.getTexto());
                    dto.setCreatedAt(mensaje.getCreatedAt());
                    /* Para el admin no hay "propio": solo quién habló. */
                    dto.setEsPropio(Mensaje.AUTOR_PUBLICADOR.equals(mensaje.getAutor()));
                    dto.setOculto(MENSAJE_OCULTO.equals(mensaje.getEstado()));
                    return dto;
                })
                .toList();
    }

    /* ===================== interno ===================== */

    private void agregarMensaje(
            Long conversacionId,
            String autor,
            String texto,
            OffsetDateTime cuando
    ) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacionId(conversacionId);
        mensaje.setAutor(autor);
        mensaje.setTexto(texto);
        mensaje.setEstado(MENSAJE_VISIBLE);
        mensaje.setCreatedAt(cuando);

        mensajeRepository.saveAndFlush(mensaje);
    }

    /**
     * Arma los DTO de una bandeja con queries batch: identidad de la
     * contraparte, actividad, no leídos y vista previa.
     */
    private List<ConversacionDTO> armar(
            List<Conversacion> conversaciones,
            String autorPropio
    ) {
        if (conversaciones.isEmpty()) {
            return List.of();
        }

        boolean miraElPublicador = Mensaje.AUTOR_PUBLICADOR.equals(autorPropio);

        List<Long> ids = conversaciones.stream().map(Conversacion::getId).toList();

        Map<Long, Long> noLeidos = new HashMap<>();
        for (MensajeRepository.ConteoNoLeidos conteo
                : mensajeRepository.contarNoLeidos(ids, autorPropio)) {
            noLeidos.put(conteo.getConversacionId(), conteo.getCantidad());
        }

        List<Long> perfilIds = conversaciones.stream()
                .map(Conversacion::getPerfilPublicadorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PerfilPublicador> perfiles = new HashMap<>();
        perfilPublicadorRepository.findAllById(perfilIds)
                .forEach(perfil -> perfiles.put(perfil.getId(), perfil));
        Map<Long, String> logos = imagenService.obtenerLogosAprobadosPorPerfil(perfilIds);

        Map<Long, Usuario> usuarios = new HashMap<>();
        if (miraElPublicador) {
            usuarioRepository.findAllById(
                    conversaciones.stream()
                            .map(Conversacion::getUsuarioId)
                            .distinct()
                            .toList()
            ).forEach(usuario -> usuarios.put(usuario.getId(), usuario));
        }

        List<Long> actividadIds = conversaciones.stream()
                .map(Conversacion::getActividadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Actividad> actividades = new HashMap<>();
        if (!actividadIds.isEmpty()) {
            actividadRepository.findAllById(actividadIds)
                    .forEach(actividad -> actividades.put(actividad.getId(), actividad));
        }

        return conversaciones.stream().map(conversacion -> {
            ConversacionDTO dto = new ConversacionDTO();
            dto.setId(conversacion.getId());
            dto.setEstado(conversacion.getEstado());
            dto.setUltimoMensajeAt(conversacion.getUltimoMensajeAt());
            dto.setNoLeidos(noLeidos.getOrDefault(conversacion.getId(), 0L));

            PerfilPublicador perfil = perfiles.get(conversacion.getPerfilPublicadorId());
            dto.setPerfilPublicadorId(conversacion.getPerfilPublicadorId());

            if (miraElPublicador) {
                /* El otro lado es la persona: nombre corto, nada más. */
                dto.setContraparteNombre(
                        nombreCorto(usuarios.get(conversacion.getUsuarioId())));
            } else if (perfil != null) {
                dto.setContraparteNombre(perfil.getNombre());
                dto.setPerfilSlug(perfil.getSlug());
                dto.setContraparteLogoUrl(logos.get(perfil.getId()));
            }

            Actividad actividad = conversacion.getActividadId() != null
                    ? actividades.get(conversacion.getActividadId())
                    : null;
            if (actividad != null) {
                dto.setActividadId(actividad.getId());
                dto.setActividadTitulo(actividad.getTitulo());
                dto.setActividadSlug(actividad.getSlug());
            }

            dto.setUltimoMensajeTexto(vistaPreviaDe(conversacion.getId()));

            return dto;
        }).toList();
    }

    /**
     * El arranque del último mensaje visible. Un mensaje ocultado por
     * el admin no se muestra en la vista previa: la bandeja no es
     * lugar para volver a mostrar lo que se moderó.
     */
    private String vistaPreviaDe(Long conversacionId) {
        List<Mensaje> mensajes =
                mensajeRepository.findByConversacionIdOrderByCreatedAtAsc(conversacionId);

        for (int indice = mensajes.size() - 1; indice >= 0; indice--) {
            Mensaje mensaje = mensajes.get(indice);

            if (MENSAJE_VISIBLE.equals(mensaje.getEstado())) {
                String texto = mensaje.getTexto();
                return texto.length() <= LARGO_VISTA_PREVIA
                        ? texto
                        : texto.substring(0, LARGO_VISTA_PREVIA) + "...";
            }
        }

        return null;
    }

    private MensajeDTO toMensajeDTO(Mensaje mensaje, String autorPropio) {
        MensajeDTO dto = new MensajeDTO();
        dto.setId(mensaje.getId());
        dto.setCreatedAt(mensaje.getCreatedAt());
        dto.setEsPropio(autorPropio.equals(mensaje.getAutor()));

        boolean oculto = MENSAJE_OCULTO.equals(mensaje.getEstado());
        dto.setOculto(oculto);
        /* Se deja el hueco visible, pero el texto moderado no vuelve. */
        dto.setTexto(oculto ? null : mensaje.getTexto());

        return dto;
    }

    /** 404 y no 403: no se delata que la conversación ajena existe. */
    private Conversacion exigirDelUsuario(Long usuarioId, Long conversacionId) {
        validarUsuario(usuarioId);

        return conversacionRepository.findById(conversacionId)
                .filter(conversacion -> conversacion.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la conversacion."
                ));
    }

    private Conversacion exigirDelPublicador(Long userId, Long conversacionId) {
        PerfilPublicador perfil = obtenerPerfil(userId);

        return conversacionRepository.findById(conversacionId)
                .filter(conversacion ->
                        conversacion.getPerfilPublicadorId().equals(perfil.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la conversacion."
                ));
    }

    /** La actividad, si se indicó, tiene que ser de ese publicador. */
    private Long validarActividadDelPerfil(Long perfilPublicadorId, Long actividadId) {
        if (actividadId == null) {
            return null;
        }

        return actividadRepository.findById(actividadId)
                .filter(actividad -> actividad.getPerfilPublicador() != null
                        && perfilPublicadorId.equals(
                                actividad.getPerfilPublicador().getId()))
                .map(Actividad::getId)
                /* No es de ese publicador: se consulta al club en general. */
                .orElse(null);
    }

    private String nombreCorto(Usuario usuario) {
        if (usuario == null) {
            return "Alguien de la comunidad";
        }

        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";

        if (nombre.isEmpty()) {
            return "Alguien de la comunidad";
        }

        return apellido.isEmpty()
                ? nombre
                : nombre + " " + apellido.substring(0, 1).toUpperCase(Locale.ROOT) + ".";
    }

    private PerfilPublicador obtenerPerfil(Long userId) {
        validarUsuario(userId);

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Perfil publicador no encontrado."
                ));
    }

    private String exigirTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new FiltroInvalidoException("El mensaje no puede estar vacio.");
        }

        String limpio = texto.trim();
        return limpio.length() <= MAX_TEXTO ? limpio : limpio.substring(0, MAX_TEXTO);
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
