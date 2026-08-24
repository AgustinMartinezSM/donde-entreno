package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.InteresActividad;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.InteresActividadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * El flujo propio (script 29): Guardé → QUIERO_PROBAR → YA_PROBE.
 * Upsert idempotente por el UNIQUE; quitar borra la fila. Solo sobre
 * actividades públicas (404 sin delatar, patrón likes).
 */
@Service
public class InteresActividadService {

    public static final String QUIERO_PROBAR = "QUIERO_PROBAR";
    public static final String YA_PROBE = "YA_PROBE";

    private static final List<String> ESTADOS = List.of(QUIERO_PROBAR, YA_PROBE);
    private static final String MENSAJE_NO_ENCONTRADA = "No se encontro la actividad.";

    private final InteresActividadRepository interesActividadRepository;
    private final ActividadRepository actividadRepository;

    public InteresActividadService(
            InteresActividadRepository interesActividadRepository,
            ActividadRepository actividadRepository
    ) {
        this.interesActividadRepository = interesActividadRepository;
        this.actividadRepository = actividadRepository;
    }

    @Transactional
    public String marcar(Long usuarioId, Long actividadId, String estado) {
        validarUserId(usuarioId);

        if (estado == null || !ESTADOS.contains(estado)) {
            throw new FiltroInvalidoException("El estado del interes no es valido.");
        }

        validarActividadPublica(actividadId);

        Optional<InteresActividad> existente =
                interesActividadRepository.findByUsuarioIdAndActividadId(usuarioId, actividadId);

        OffsetDateTime ahora = OffsetDateTime.now();

        if (existente.isPresent()) {
            InteresActividad interes = existente.get();
            interes.setEstado(estado);
            interes.setUpdatedAt(ahora);
            interesActividadRepository.save(interes);
            return estado;
        }

        InteresActividad interes = new InteresActividad();
        interes.setUsuarioId(usuarioId);
        interes.setActividadId(actividadId);
        interes.setEstado(estado);
        interes.setCreatedAt(ahora);
        interes.setUpdatedAt(ahora);

        try {
            interesActividadRepository.saveAndFlush(interes);
        } catch (DataIntegrityViolationException excepcion) {
            /* Carrera del UNIQUE: otro request lo marcó — mismo resultado. */
        }

        return estado;
    }

    @Transactional
    public void quitar(Long usuarioId, Long actividadId) {
        validarUserId(usuarioId);
        interesActividadRepository.deleteByUsuarioIdAndActividadId(usuarioId, actividadId);
    }

    /** Estado propio para pintar el control ("QUIERO_PROBAR" | "YA_PROBE" | null). */
    @Transactional(readOnly = true)
    public String estadoDe(Long usuarioId, Long actividadId) {
        validarUserId(usuarioId);
        return interesActividadRepository
                .findByUsuarioIdAndActividadId(usuarioId, actividadId)
                .map(InteresActividad::getEstado)
                .orElse(null);
    }

    /** Contador agregado y anónimo: cuántos quieren probar. */
    @Transactional(readOnly = true)
    public long contarQuierenProbar(Long actividadId) {
        return interesActividadRepository
                .countByActividadIdAndEstado(actividadId, QUIERO_PROBAR);
    }

    /** Señal FUERTE para la insignia Verificada de valoraciones. */
    @Transactional(readOnly = true)
    public boolean yaProbo(Long usuarioId, Long actividadId) {
        return interesActividadRepository
                .existsByUsuarioIdAndActividadIdAndEstado(usuarioId, actividadId, YA_PROBE);
    }

    /** Señal de uso mínima (cualquier interés) para poder valorar. */
    @Transactional(readOnly = true)
    public boolean tieneInteres(Long usuarioId, Long actividadId) {
        return interesActividadRepository
                .findByUsuarioIdAndActividadId(usuarioId, actividadId)
                .isPresent();
    }

    private void validarActividadPublica(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .filter(encontrada -> Boolean.TRUE.equals(encontrada.getActiva())
                        && "PUBLICADA".equals(encontrada.getEstadoPublicacion())
                        && encontrada.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA));

        if (actividad.getId() == null) {
            throw new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA);
        }
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
