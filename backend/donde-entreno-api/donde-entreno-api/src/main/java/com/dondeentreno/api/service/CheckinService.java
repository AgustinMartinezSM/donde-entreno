package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.CheckinRespuestaDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.EntrenamientoUsuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EntrenamientoUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Check-in "Entrené acá" (script 26, plan de valoraciones etapa A).
 *
 * Reglas del plan: 1 check-in por actividad por día, validado CONTRA LA
 * BASE (Render reinicia y en rotación hay dos instancias: la memoria
 * dejaría colar duplicados), con el día calculado en zona argentina
 * (en UTC, a las 22:00 de acá ya es "mañana" y permitiría el doble).
 * Solo sobre actividades públicas: lo no visible da 404, no 403, para
 * no delatar que existe (patrón LikesFotosService).
 */
@Service
public class CheckinService {

    /** El producto es local (Mar del Plata): el "día" es el argentino. */
    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String ESTADO_PUBLICADA = "PUBLICADA";
    private static final String MENSAJE_ACTIVIDAD_NO_ENCONTRADA = "No se encontro la actividad.";
    private static final int DIAS_VENTANA_CONTADOR = 30;

    private final EntrenamientoUsuarioRepository entrenamientoUsuarioRepository;
    private final ActividadRepository actividadRepository;

    public CheckinService(
            EntrenamientoUsuarioRepository entrenamientoUsuarioRepository,
            ActividadRepository actividadRepository
    ) {
        this.entrenamientoUsuarioRepository = entrenamientoUsuarioRepository;
        this.actividadRepository = actividadRepository;
    }

    /**
     * Registra el check-in de hoy. Si ya existe uno hoy para esta
     * actividad, no crea fila (idempotente: el controller responde 200
     * en vez de 201, y el mensaje del botón no cambia).
     */
    @Transactional
    public CheckinRespuestaDTO registrar(Long usuarioId, Long actividadId) {
        validarUserId(usuarioId);
        validarActividadPublica(actividadId);

        OffsetDateTime inicioDeHoy = inicioDeHoy();
        boolean yaRegistradoHoy = entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        usuarioId, actividadId, inicioDeHoy);

        boolean registradoAhora = false;
        if (!yaRegistradoHoy) {
            EntrenamientoUsuario checkin = new EntrenamientoUsuario();
            checkin.setUsuarioId(usuarioId);
            checkin.setActividadId(actividadId);
            checkin.setCreatedAt(OffsetDateTime.now());
            entrenamientoUsuarioRepository.saveAndFlush(checkin);
            registradoAhora = true;
        }

        return new CheckinRespuestaDTO(
                true,
                registradoAhora,
                contarPersonas30Dias(actividadId)
        );
    }

    /** Estado del botón al cargar el detalle logueado. */
    @Transactional(readOnly = true)
    public CheckinRespuestaDTO estadoDeHoy(Long usuarioId, Long actividadId) {
        validarUserId(usuarioId);
        validarActividadPublica(actividadId);

        boolean yaRegistradoHoy = entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        usuarioId, actividadId, inicioDeHoy());

        return new CheckinRespuestaDTO(
                yaRegistradoHoy,
                false,
                contarPersonas30Dias(actividadId)
        );
    }

    /**
     * Personas DISTINTAS de los últimos 30 días — el contador público
     * ("N personas entrenaron acá este mes"). Agregado y anónimo.
     */
    @Transactional(readOnly = true)
    public long contarPersonas30Dias(Long actividadId) {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(DIAS_VENTANA_CONTADOR);
        return entrenamientoUsuarioRepository.contarPersonasDesde(actividadId, desde);
    }

    private void validarActividadPublica(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_ACTIVIDAD_NO_ENCONTRADA));

        if (!Boolean.TRUE.equals(actividad.getActiva())
                || !ESTADO_PUBLICADA.equals(actividad.getEstadoPublicacion())
                || actividad.getDeletedAt() != null) {
            throw new RecursoNoEncontradoException(MENSAJE_ACTIVIDAD_NO_ENCONTRADA);
        }
    }

    private OffsetDateTime inicioDeHoy() {
        return LocalDate.now(ZONA_ARGENTINA)
                .atStartOfDay(ZONA_ARGENTINA)
                .toOffsetDateTime();
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
