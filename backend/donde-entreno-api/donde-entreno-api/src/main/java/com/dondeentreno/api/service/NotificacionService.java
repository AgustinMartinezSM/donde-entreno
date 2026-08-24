package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.NotificacionDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.entity.Notificacion;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Notificaciones internas (script 28, Fase 2 social).
 *
 * La emisión NUNCA rompe el flujo que la origina: si guardar la
 * notificación falla, se loguea y el negocio sigue — aprobar una
 * actividad vale más que avisarla.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final int MAX_TITULO = 150;
    private static final int MAX_PAGINA = 50;

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Emite una notificación para un usuario. Best-effort: nunca lanza.
     * REQUIRES_NEW para que un rollback del flujo de negocio no deje
     * notificaciones fantasma NI un fallo acá voltee al negocio.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void emitir(Long usuarioId, String tipo, String titulo, String ruta) {
        try {
            if (usuarioId == null || tipo == null || titulo == null) {
                return;
            }

            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuarioId);
            notificacion.setTipo(tipo);
            notificacion.setTitulo(recortar(titulo));
            notificacion.setRuta(ruta);
            notificacion.setLeida(Boolean.FALSE);
            notificacion.setCreatedAt(OffsetDateTime.now());

            notificacionRepository.save(notificacion);
        } catch (RuntimeException excepcion) {
            log.warn("NOTIFICACION_NO_EMITIDA tipo={} usuario={}: {}",
                    tipo, usuarioId, excepcion.getMessage());
        }
    }

    /**
     * Fan-out a muchos usuarios (p. ej. los seguidores de un perfil al
     * aprobarse una actividad nueva). Mismo contrato best-effort.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void emitirATodos(
            Collection<Long> usuarioIds,
            String tipo,
            String titulo,
            String ruta
    ) {
        try {
            if (usuarioIds == null || usuarioIds.isEmpty() || tipo == null || titulo == null) {
                return;
            }

            OffsetDateTime ahora = OffsetDateTime.now();
            List<Notificacion> notificaciones = new ArrayList<>(usuarioIds.size());

            for (Long usuarioId : usuarioIds) {
                if (usuarioId == null) {
                    continue;
                }

                Notificacion notificacion = new Notificacion();
                notificacion.setUsuarioId(usuarioId);
                notificacion.setTipo(tipo);
                notificacion.setTitulo(recortar(titulo));
                notificacion.setRuta(ruta);
                notificacion.setLeida(Boolean.FALSE);
                notificacion.setCreatedAt(ahora);
                notificaciones.add(notificacion);
            }

            notificacionRepository.saveAll(notificaciones);
        } catch (RuntimeException excepcion) {
            log.warn("NOTIFICACION_FANOUT_NO_EMITIDO tipo={} destinatarios={}: {}",
                    tipo, usuarioIds.size(), excepcion.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<NotificacionDTO> listar(Long usuarioId, int page, int size) {
        validarUserId(usuarioId);

        int tamanio = Math.min(Math.max(size, 1), MAX_PAGINA);
        Page<Notificacion> pagina = notificacionRepository
                .findByUsuarioIdOrderByCreatedAtDesc(
                        usuarioId,
                        PageRequest.of(Math.max(page, 0), tamanio)
                );

        return new PaginaResponseDTO<>(
                pagina.getContent().stream().map(this::toDTO).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isLast()
        );
    }

    /** El número de la campanita. */
    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        validarUserId(usuarioId);
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /** Marca UNA como leída; solo la propia (404 si es de otro). */
    @Transactional
    public void marcarLeida(Long usuarioId, Long notificacionId) {
        validarUserId(usuarioId);

        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .filter(encontrada -> encontrada.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la notificacion."));

        notificacion.setLeida(Boolean.TRUE);
        notificacionRepository.save(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        validarUserId(usuarioId);
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }

    private NotificacionDTO toDTO(Notificacion notificacion) {
        return new NotificacionDTO(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getRuta(),
                Boolean.TRUE.equals(notificacion.getLeida()),
                notificacion.getCreatedAt()
        );
    }

    private String recortar(String titulo) {
        return titulo.length() <= MAX_TITULO
                ? titulo
                : titulo.substring(0, MAX_TITULO);
    }

    private void validarUserId(Long usuarioId) {
        if (usuarioId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }
    }
}
