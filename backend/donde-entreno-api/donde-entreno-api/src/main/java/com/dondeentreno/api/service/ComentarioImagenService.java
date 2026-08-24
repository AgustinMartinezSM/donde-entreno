package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ComentarioImagenDTO;
import com.dondeentreno.api.entity.ComentarioImagen;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ComentarioImagenRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comentarios en fotos (script 30): el primer texto libre de la
 * comunidad. Publica DIRECTO (filosofía flexible) y se modera por
 * estados: el autor elimina lo suyo, el DUEÑO de la foto oculta en su
 * contenido, el admin oculta cualquiera; los reportes son la señal.
 */
@Service
public class ComentarioImagenService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String ESTADO_VISIBLE = "VISIBLE";
    private static final String ESTADO_OCULTO_PUBLICADOR = "OCULTO_POR_PUBLICADOR";
    private static final String ESTADO_OCULTO_ADMIN = "OCULTO_POR_ADMIN";
    private static final String ESTADO_ELIMINADO = "ELIMINADO_POR_USUARIO";
    private static final String ESTADO_MODERACION_APROBADA = "APROBADA";
    private static final int MAX_COMENTARIOS_POR_DIA = 20;
    private static final int MAX_TEXTO = 500;
    private static final String MENSAJE_FOTO_NO_ENCONTRADA = "No se encontro la foto.";

    private final ComentarioImagenRepository comentarioImagenRepository;
    private final ImagenRepository imagenRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public ComentarioImagenService(
            ComentarioImagenRepository comentarioImagenRepository,
            ImagenRepository imagenRepository,
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService
    ) {
        this.comentarioImagenRepository = comentarioImagenRepository;
        this.imagenRepository = imagenRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ComentarioImagenDTO comentar(Long usuarioId, Long imagenId, String texto) {
        validarUserId(usuarioId);

        String textoLimpio = texto != null ? texto.trim() : "";
        if (textoLimpio.isEmpty()) {
            throw new FiltroInvalidoException("El comentario no puede estar vacio.");
        }
        if (textoLimpio.length() > MAX_TEXTO) {
            textoLimpio = textoLimpio.substring(0, MAX_TEXTO);
        }

        Imagen imagen = buscarFotoVisible(imagenId);

        if (!Boolean.TRUE.equals(imagen.getComentariosActivados())) {
            throw new FiltroInvalidoException(
                    "El publicador desactivo los comentarios en esta foto."
            );
        }

        /* Tope diario contra la base (día argentino, patrón preguntas). */
        OffsetDateTime inicioDeHoy = LocalDate.now(ZONA_ARGENTINA)
                .atStartOfDay(ZONA_ARGENTINA)
                .toOffsetDateTime();
        if (comentarioImagenRepository
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(usuarioId, inicioDeHoy)
                >= MAX_COMENTARIOS_POR_DIA) {
            throw new FiltroInvalidoException(
                    "Llegaste al tope de comentarios por hoy. Probá de nuevo mañana."
            );
        }

        ComentarioImagen comentario = new ComentarioImagen();
        comentario.setImagenId(imagenId);
        comentario.setUsuarioId(usuarioId);
        comentario.setTexto(textoLimpio);
        comentario.setEstado(ESTADO_VISIBLE);
        comentario.setCreatedAt(OffsetDateTime.now());

        ComentarioImagen guardado = comentarioImagenRepository.save(comentario);

        notificarDuenioDeLaFoto(imagen);

        return toDTO(guardado, resolverNombre(usuarioId), usuarioId);
    }

    /** Listado público de visibles, autores resueltos en batch. */
    @Transactional(readOnly = true)
    public List<ComentarioImagenDTO> listarDe(Long imagenId, Long usuarioActualId) {
        List<ComentarioImagen> visibles = comentarioImagenRepository
                .findByImagenIdAndEstadoOrderByCreatedAtAsc(imagenId, ESTADO_VISIBLE);

        List<Long> autorIds = visibles.stream()
                .map(ComentarioImagen::getUsuarioId)
                .distinct()
                .toList();
        Map<Long, String> nombres = usuarioRepository.findAllById(autorIds).stream()
                .collect(Collectors.toMap(Usuario::getId, this::nombreCorto));

        return visibles.stream()
                .map(comentario -> toDTO(
                        comentario,
                        nombres.getOrDefault(comentario.getUsuarioId(), "Alguien de la comunidad"),
                        usuarioActualId
                ))
                .toList();
    }

    @Transactional
    public void eliminarPropio(Long usuarioId, Long comentarioId) {
        validarUserId(usuarioId);

        ComentarioImagen comentario = comentarioImagenRepository.findById(comentarioId)
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .filter(encontrado -> ESTADO_VISIBLE.equals(encontrado.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el comentario."));

        comentario.setEstado(ESTADO_ELIMINADO);
        comentarioImagenRepository.save(comentario);
    }

    /** El DUEÑO de la foto oculta comentarios en su contenido. */
    @Transactional
    public void ocultarPorPublicador(Long usuarioId, Long comentarioId) {
        validarUserId(usuarioId);

        ComentarioImagen comentario = comentarioImagenRepository.findById(comentarioId)
                .filter(encontrado -> ESTADO_VISIBLE.equals(encontrado.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el comentario."));

        Imagen imagen = imagenRepository.findById(comentario.getImagenId())
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_FOTO_NO_ENCONTRADA));

        if (!esDuenioDeLaFoto(imagen, usuarioId)) {
            /* 404, no 403: no se delata (patrón likes). */
            throw new RecursoNoEncontradoException("No se encontro el comentario.");
        }

        comentario.setEstado(ESTADO_OCULTO_PUBLICADOR);
        comentarioImagenRepository.save(comentario);
    }

    @Transactional
    public void ocultarPorAdmin(Long comentarioId) {
        ComentarioImagen comentario = comentarioImagenRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el comentario."));

        comentario.setEstado(ESTADO_OCULTO_ADMIN);
        comentarioImagenRepository.save(comentario);
    }

    /** ¿Visible? (para reportarlo). */
    @Transactional(readOnly = true)
    public boolean esVisible(Long comentarioId) {
        return comentarioImagenRepository.findById(comentarioId)
                .filter(comentario -> ESTADO_VISIBLE.equals(comentario.getEstado()))
                .isPresent();
    }

    private void notificarDuenioDeLaFoto(Imagen imagen) {
        Long duenioId = null;
        String ruta = "/publicador/fotos";

        if (imagen.getActividad() != null
                && imagen.getActividad().getPerfilPublicador() != null
                && imagen.getActividad().getPerfilPublicador().getUsuario() != null) {
            duenioId = imagen.getActividad().getPerfilPublicador().getUsuario().getId();
            ruta = "/actividades/" + imagen.getActividad().getSlug();
        } else if (imagen.getPerfilPublicador() != null
                && imagen.getPerfilPublicador().getUsuario() != null) {
            duenioId = imagen.getPerfilPublicador().getUsuario().getId();
        }

        if (duenioId != null) {
            notificacionService.emitir(
                    duenioId,
                    "COMENTARIO_NUEVO",
                    "Comentaron una de tus fotos.",
                    ruta
            );
        }
    }

    private boolean esDuenioDeLaFoto(Imagen imagen, Long usuarioId) {
        if (imagen.getActividad() != null
                && imagen.getActividad().getPerfilPublicador() != null
                && imagen.getActividad().getPerfilPublicador().getUsuario() != null) {
            return imagen.getActividad().getPerfilPublicador().getUsuario().getId()
                    .equals(usuarioId);
        }

        return imagen.getPerfilPublicador() != null
                && imagen.getPerfilPublicador().getUsuario() != null
                && imagen.getPerfilPublicador().getUsuario().getId().equals(usuarioId);
    }

    private Imagen buscarFotoVisible(Long imagenId) {
        return imagenRepository.findById(imagenId)
                .filter(imagen -> Boolean.TRUE.equals(imagen.getActiva())
                        && ESTADO_MODERACION_APROBADA.equals(imagen.getEstadoModeracion()))
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_FOTO_NO_ENCONTRADA));
    }

    private ComentarioImagenDTO toDTO(
            ComentarioImagen comentario,
            String autorNombre,
            Long usuarioActualId
    ) {
        return new ComentarioImagenDTO(
                comentario.getId(),
                comentario.getTexto(),
                autorNombre,
                comentario.getUsuarioId().equals(usuarioActualId),
                comentario.getCreatedAt()
        );
    }

    private String resolverNombre(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(this::nombreCorto)
                .orElse("Alguien de la comunidad");
    }

    private String nombreCorto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";

        if (nombre.isEmpty()) {
            return "Alguien de la comunidad";
        }

        return apellido.isEmpty()
                ? nombre
                : nombre + " " + apellido.substring(0, 1).toUpperCase(Locale.ROOT) + ".";
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
