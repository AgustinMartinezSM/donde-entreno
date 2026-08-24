package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ResumenValoracionesDTO;
import com.dondeentreno.api.dto.ValoracionPublicaDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.entity.Valoracion;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EntrenamientoUsuarioRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.repository.ValoracionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Valoraciones 1-5 con reseña (script 29, Fase 3 social).
 *
 * Reglas del plan: valorar exige SEÑAL DE USO sobre esa actividad
 * (guardado, interés o check-in); la insignia Verificada queda solo
 * con señal fuerte (YA_PROBE o check-in); el promedio aparece recién
 * con 3+ valoraciones; el texto publica directo y se modera por
 * reportes (estado OCULTA_POR_ADMIN).
 */
@Service
public class ValoracionService {

    public static final List<String> TAGS_PERMITIDOS = List.of(
            "BUEN_AMBIENTE",
            "IDEAL_PRINCIPIANTES",
            "PROFES_ATENTOS",
            "BUENA_UBICACION",
            "MUY_INTENSO",
            "INSTALACIONES_COMODAS"
    );

    private static final String ESTADO_VISIBLE = "VISIBLE";
    private static final String ESTADO_OCULTA = "OCULTA_POR_ADMIN";
    private static final int MINIMO_PARA_PROMEDIO = 3;
    private static final int MAX_COMENTARIO = 500;
    private static final int MAX_PAGINA = 50;
    private static final String MENSAJE_NO_ENCONTRADA = "No se encontro la actividad.";

    private final ValoracionRepository valoracionRepository;
    private final ActividadRepository actividadRepository;
    private final FavoritoActividadRepository favoritoActividadRepository;
    private final InteresActividadService interesActividadService;
    private final EntrenamientoUsuarioRepository entrenamientoUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public ValoracionService(
            ValoracionRepository valoracionRepository,
            ActividadRepository actividadRepository,
            FavoritoActividadRepository favoritoActividadRepository,
            InteresActividadService interesActividadService,
            EntrenamientoUsuarioRepository entrenamientoUsuarioRepository,
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService
    ) {
        this.valoracionRepository = valoracionRepository;
        this.actividadRepository = actividadRepository;
        this.favoritoActividadRepository = favoritoActividadRepository;
        this.interesActividadService = interesActividadService;
        this.entrenamientoUsuarioRepository = entrenamientoUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ValoracionPublicaDTO valorar(
            Long usuarioId,
            Long actividadId,
            Integer puntaje,
            String comentario,
            List<String> tags
    ) {
        validarUserId(usuarioId);

        if (puntaje == null || puntaje < 1 || puntaje > 5) {
            throw new FiltroInvalidoException("El puntaje debe estar entre 1 y 5.");
        }

        Actividad actividad = buscarActividadPublica(actividadId);

        /* 400 con mensaje claro: el frontend lo muestra tal cual. */
        if (!tieneSenalDeUso(usuarioId, actividadId)) {
            throw new FiltroInvalidoException(
                    "Para valorar esta actividad primero guardala, marcá que querés "
                            + "probarla, que ya la probaste o que entrenás ahí."
            );
        }

        boolean verificada = tieneSenalFuerte(usuarioId, actividadId);
        OffsetDateTime ahora = OffsetDateTime.now();

        Valoracion valoracion = valoracionRepository
                .findByUsuarioIdAndActividadId(usuarioId, actividadId)
                .orElseGet(() -> {
                    Valoracion nueva = new Valoracion();
                    nueva.setUsuarioId(usuarioId);
                    nueva.setActividadId(actividadId);
                    nueva.setCreatedAt(ahora);
                    return nueva;
                });

        boolean esNueva = valoracion.getId() == null;

        valoracion.setPuntaje(puntaje);
        valoracion.setComentario(normalizarComentario(comentario));
        valoracion.setTags(normalizarTags(tags));
        valoracion.setVerificada(verificada);
        /* Editar la propia la vuelve visible salvo que la haya ocultado el admin. */
        if (esNueva) {
            valoracion.setEstado(ESTADO_VISIBLE);
        }
        valoracion.setUpdatedAt(ahora);

        Valoracion guardada = valoracionRepository.save(valoracion);

        if (esNueva && actividad.getPerfilPublicador() != null
                && actividad.getPerfilPublicador().getUsuario() != null) {
            notificacionService.emitir(
                    actividad.getPerfilPublicador().getUsuario().getId(),
                    "VALORACION_NUEVA",
                    "Tu actividad \"" + actividad.getTitulo() + "\" recibió una valoración de "
                            + puntaje + " estrellas.",
                    "/actividades/" + actividad.getSlug() + "#valoraciones"
            );
        }

        return toPublicaDTO(guardada, resolverAutor(usuarioId), usuarioId);
    }

    @Transactional
    public void eliminarPropia(Long usuarioId, Long actividadId) {
        validarUserId(usuarioId);
        valoracionRepository.deleteByUsuarioIdAndActividadId(usuarioId, actividadId);
    }

    /** Moderación flexible: el admin oculta, no borra (queda el rastro). */
    @Transactional
    public void ocultarPorAdmin(Long valoracionId) {
        Valoracion valoracion = valoracionRepository.findById(valoracionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la valoracion."));

        valoracion.setEstado(ESTADO_OCULTA);
        valoracion.setUpdatedAt(OffsetDateTime.now());
        valoracionRepository.save(valoracion);
    }

    /**
     * Resumen público. usuarioId puede ser null (visitante): esPropia
     * queda en false y no se filtra nada.
     */
    @Transactional(readOnly = true)
    public ResumenValoracionesDTO resumenDe(Long actividadId, Long usuarioId, int page, int size) {
        int tamanio = Math.min(Math.max(size, 1), MAX_PAGINA);
        Page<Valoracion> pagina = valoracionRepository
                .findByActividadIdAndEstadoOrderByCreatedAtDesc(
                        actividadId,
                        ESTADO_VISIBLE,
                        PageRequest.of(Math.max(page, 0), tamanio)
                );

        /*
          La cantidad sale de la DISTRIBUCIÓN y no del total de la
          página: es la misma fuente que el promedio (consistencia
          garantizada) y evita el ajuste de totales de PageImpl.
        */
        Map<Integer, Long> distribucion = new HashMap<>();
        long cantidad = 0;
        long suma = 0;
        for (Object[] fila : valoracionRepository.distribucionVisibles(actividadId)) {
            Integer puntaje = (Integer) fila[0];
            Long conteo = (Long) fila[1];
            distribucion.put(puntaje, conteo);
            cantidad += conteo;
            suma += (long) puntaje * conteo;
        }

        Double promedio = cantidad >= MINIMO_PARA_PROMEDIO
                ? Math.round((double) suma / cantidad * 10.0) / 10.0
                : null;

        /* Autores en batch: nombre de pila + inicial, sin N+1. */
        List<Long> autorIds = pagina.getContent().stream()
                .map(Valoracion::getUsuarioId)
                .distinct()
                .toList();
        Map<Long, String> nombres = usuarioRepository.findAllById(autorIds).stream()
                .collect(Collectors.toMap(Usuario::getId, this::nombreCorto));

        List<ValoracionPublicaDTO> contenido = pagina.getContent().stream()
                .map(valoracion -> toPublicaDTO(
                        valoracion,
                        nombres.getOrDefault(valoracion.getUsuarioId(), "Alguien de la comunidad"),
                        usuarioId
                ))
                .toList();

        return new ResumenValoracionesDTO(promedio, cantidad, distribucion, contenido);
    }

    /** Para el social proof del detalle: promedio (null si N<3) y cantidad. */
    @Transactional(readOnly = true)
    public double[] promedioYCantidad(Long actividadId) {
        long cantidad = valoracionRepository.countByActividadIdAndEstado(actividadId, ESTADO_VISIBLE);

        if (cantidad < MINIMO_PARA_PROMEDIO) {
            return new double[]{-1, cantidad};
        }

        long suma = 0;
        for (Object[] fila : valoracionRepository.distribucionVisibles(actividadId)) {
            suma += (long) (Integer) fila[0] * (Long) fila[1];
        }

        return new double[]{Math.round((double) suma / cantidad * 10.0) / 10.0, cantidad};
    }

    /** ¿La valoración está visible? (para reportarla). */
    @Transactional(readOnly = true)
    public boolean esVisible(Long valoracionId) {
        return valoracionRepository.findById(valoracionId)
                .filter(valoracion -> ESTADO_VISIBLE.equals(valoracion.getEstado()))
                .isPresent();
    }

    private boolean tieneSenalDeUso(Long usuarioId, Long actividadId) {
        return favoritoActividadRepository.existsByUsuarioIdAndActividadId(usuarioId, actividadId)
                || interesActividadService.tieneInteres(usuarioId, actividadId)
                || tieneCheckin(usuarioId, actividadId);
    }

    private boolean tieneSenalFuerte(Long usuarioId, Long actividadId) {
        return interesActividadService.yaProbo(usuarioId, actividadId)
                || tieneCheckin(usuarioId, actividadId);
    }

    private boolean tieneCheckin(Long usuarioId, Long actividadId) {
        /* Cualquier check-in histórico cuenta como señal fuerte. */
        return entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        usuarioId,
                        actividadId,
                        OffsetDateTime.parse("2020-01-01T00:00:00Z")
                );
    }

    private Actividad buscarActividadPublica(Long actividadId) {
        return actividadRepository.findById(actividadId)
                .filter(actividad -> Boolean.TRUE.equals(actividad.getActiva())
                        && "PUBLICADA".equals(actividad.getEstadoPublicacion())
                        && actividad.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA));
    }

    private ValoracionPublicaDTO toPublicaDTO(
            Valoracion valoracion,
            String autorNombre,
            Long usuarioActualId
    ) {
        return new ValoracionPublicaDTO(
                valoracion.getId(),
                valoracion.getPuntaje(),
                valoracion.getComentario(),
                parsearTags(valoracion.getTags()),
                Boolean.TRUE.equals(valoracion.getVerificada()),
                autorNombre,
                valoracion.getUsuarioId().equals(usuarioActualId),
                valoracion.getCreatedAt()
        );
    }

    private String resolverAutor(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(this::nombreCorto)
                .orElse("Alguien de la comunidad");
    }

    /* "Agustín Martínez" → "Agustín M.": acto público, apellido privado. */
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

    private String normalizarComentario(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }

        String limpio = comentario.trim();
        return limpio.length() <= MAX_COMENTARIO ? limpio : limpio.substring(0, MAX_COMENTARIO);
    }

    private String normalizarTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        List<String> validos = tags.stream()
                .filter(TAGS_PERMITIDOS::contains)
                .distinct()
                .toList();

        return validos.isEmpty() ? null : String.join(",", validos);
    }

    private List<String> parsearTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }

        return new ArrayList<>(Arrays.asList(tags.split(",")));
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
