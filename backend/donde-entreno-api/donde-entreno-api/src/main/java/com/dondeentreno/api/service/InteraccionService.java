package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.EventoInteraccion;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EventoInteraccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracking anónimo de interacciones (script 28, Fase 2 social).
 * SIN usuario a propósito: se cuentan eventos, nunca quién. El rate
 * limit por IP vive en el controller (patrón del asistente).
 */
@Service
public class InteraccionService {

    private static final List<String> TIPOS =
            List.of("VISTA_DETALLE", "CLICK_WHATSAPP", "CLICK_COMPARTIR");
    private static final int DIAS_VENTANA_METRICAS = 30;

    private final EventoInteraccionRepository eventoInteraccionRepository;
    private final ActividadRepository actividadRepository;

    public InteraccionService(
            EventoInteraccionRepository eventoInteraccionRepository,
            ActividadRepository actividadRepository
    ) {
        this.eventoInteraccionRepository = eventoInteraccionRepository;
        this.actividadRepository = actividadRepository;
    }

    @Transactional
    public void registrar(Long actividadId, String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new FiltroInvalidoException("El tipo de interaccion no es valido.");
        }

        Actividad actividad = actividadRepository.findById(actividadId)
                .filter(encontrada -> Boolean.TRUE.equals(encontrada.getActiva())
                        && "PUBLICADA".equals(encontrada.getEstadoPublicacion())
                        && encontrada.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la actividad."));

        EventoInteraccion evento = new EventoInteraccion();
        evento.setActividadId(actividad.getId());
        evento.setTipo(tipo);
        evento.setCreatedAt(OffsetDateTime.now());

        eventoInteraccionRepository.save(evento);
    }

    /**
     * Conteos de los últimos 30 días por actividad y tipo, en UN query
     * agrupado. Clave del mapa exterior: actividadId; interior: tipo.
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<String, Long>> contarUltimos30Dias(Collection<Long> actividadIds) {
        Map<Long, Map<String, Long>> conteos = new HashMap<>();

        if (actividadIds == null || actividadIds.isEmpty()) {
            return conteos;
        }

        OffsetDateTime desde = OffsetDateTime.now().minusDays(DIAS_VENTANA_METRICAS);

        for (Object[] fila : eventoInteraccionRepository
                .contarPorActividadYTipo(actividadIds, desde)) {
            Long actividadId = (Long) fila[0];
            String tipo = (String) fila[1];
            Long cantidad = (Long) fila[2];

            conteos.computeIfAbsent(actividadId, clave -> new HashMap<>())
                    .put(tipo, cantidad);
        }

        return conteos;
    }
}
