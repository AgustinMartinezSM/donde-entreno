package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ColeccionGuardadosDTO;
import com.dondeentreno.api.entity.ColeccionGuardados;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.repository.ColeccionGuardadosRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Colecciones de guardados (script 22, bloque 13).
 *
 * Todo se acota al usuario del JWT: una coleccion ajena da 404, nunca
 * 403 (no se delata que existe). Borrar una coleccion NUNCA borra
 * guardados: la FK con ON DELETE SET NULL los devuelve a "Todos".
 */
@Service
public class ColeccionesGuardadosService {

    /* Tope razonable: mas colecciones que guardados posibles es ruido. */
    static final int MAX_COLECCIONES = 20;

    private final ColeccionGuardadosRepository coleccionRepository;
    private final FavoritoActividadRepository favoritoRepository;

    public ColeccionesGuardadosService(
            ColeccionGuardadosRepository coleccionRepository,
            FavoritoActividadRepository favoritoRepository
    ) {
        this.coleccionRepository = coleccionRepository;
        this.favoritoRepository = favoritoRepository;
    }

    @Transactional(readOnly = true)
    public List<ColeccionGuardadosDTO> listar(Long usuarioId) {
        validarUserId(usuarioId);

        /* Un solo query agrupado para todos los conteos: sin N+1. */
        Map<Long, Long> conteos = new HashMap<>();
        for (Object[] fila : favoritoRepository.contarPorColeccion(usuarioId)) {
            conteos.put((Long) fila[0], (Long) fila[1]);
        }

        return coleccionRepository.findByUsuarioIdOrderByNombreAsc(usuarioId)
                .stream()
                .map(coleccion -> new ColeccionGuardadosDTO(
                        coleccion.getId(),
                        coleccion.getNombre(),
                        conteos.getOrDefault(coleccion.getId(), 0L)
                ))
                .toList();
    }

    @Transactional
    public ColeccionGuardadosDTO crear(Long usuarioId, String nombre) {
        validarUserId(usuarioId);
        String nombreNormalizado = normalizarNombre(nombre);

        if (coleccionRepository.countByUsuarioId(usuarioId) >= MAX_COLECCIONES) {
            throw new SolicitudPublicacionInvalidaException(
                    "Llegaste al maximo de " + MAX_COLECCIONES + " colecciones."
            );
        }

        if (coleccionRepository.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombreNormalizado)) {
            throw new SolicitudPublicacionInvalidaException(
                    "Ya tenes una coleccion con ese nombre."
            );
        }

        ColeccionGuardados coleccion = new ColeccionGuardados();
        coleccion.setUsuarioId(usuarioId);
        coleccion.setNombre(nombreNormalizado);
        coleccion.setCreatedAt(OffsetDateTime.now());

        ColeccionGuardados guardada = coleccionRepository.save(coleccion);

        return new ColeccionGuardadosDTO(guardada.getId(), guardada.getNombre(), 0L);
    }

    @Transactional
    public ColeccionGuardadosDTO renombrar(Long usuarioId, Long coleccionId, String nombre) {
        validarUserId(usuarioId);
        String nombreNormalizado = normalizarNombre(nombre);

        ColeccionGuardados coleccion = buscarPropia(usuarioId, coleccionId);

        if (!coleccion.getNombre().equalsIgnoreCase(nombreNormalizado)
                && coleccionRepository.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombreNormalizado)) {
            throw new SolicitudPublicacionInvalidaException(
                    "Ya tenes una coleccion con ese nombre."
            );
        }

        coleccion.setNombre(nombreNormalizado);
        ColeccionGuardados guardada = coleccionRepository.save(coleccion);

        long cantidad = favoritoRepository.contarPorColeccion(usuarioId).stream()
                .filter(fila -> guardada.getId().equals(fila[0]))
                .map(fila -> (Long) fila[1])
                .findFirst()
                .orElse(0L);

        return new ColeccionGuardadosDTO(guardada.getId(), guardada.getNombre(), cantidad);
    }

    /** La FK ON DELETE SET NULL devuelve los guardados a "Todos". */
    @Transactional
    public void eliminar(Long usuarioId, Long coleccionId) {
        validarUserId(usuarioId);
        ColeccionGuardados coleccion = buscarPropia(usuarioId, coleccionId);
        coleccionRepository.delete(coleccion);
    }

    /** Para que favoritos valide una asignacion (mismo criterio de dueño). */
    @Transactional(readOnly = true)
    public void validarPropia(Long usuarioId, Long coleccionId) {
        buscarPropia(usuarioId, coleccionId);
    }

    private ColeccionGuardados buscarPropia(Long usuarioId, Long coleccionId) {
        return coleccionRepository.findByIdAndUsuarioId(coleccionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la coleccion."
                ));
    }

    private String normalizarNombre(String nombre) {
        String limpio = nombre != null ? nombre.trim() : "";

        if (limpio.isEmpty()) {
            throw new SolicitudPublicacionInvalidaException(
                    "El nombre de la coleccion es obligatorio."
            );
        }

        return limpio;
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
