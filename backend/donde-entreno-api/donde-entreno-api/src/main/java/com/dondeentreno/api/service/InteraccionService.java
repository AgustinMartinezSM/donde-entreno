package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.EventoInteraccion;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EventoInteraccionRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
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
    private final PerfilPublicadorRepository perfilPublicadorRepository;

    public InteraccionService(
            EventoInteraccionRepository eventoInteraccionRepository,
            ActividadRepository actividadRepository,
            PerfilPublicadorRepository perfilPublicadorRepository
    ) {
        this.eventoInteraccionRepository = eventoInteraccionRepository;
        this.actividadRepository = actividadRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
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
     * Interacción sobre un PERFIL de publicador (Fase 5, script 31).
     * Va con `perfil_publicador_id` y `actividad_id` en null: colgar el
     * click del perfil de una actividad inventada ensuciaría las
     * métricas por actividad que el publicador ya está mirando.
     */
    @Transactional
    public void registrarEnPerfil(Long perfilPublicadorId, String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new FiltroInvalidoException("El tipo de interaccion no es valido.");
        }

        PerfilPublicador perfil = perfilPublicadorRepository
                .findByIdAndActivoTrue(perfilPublicadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el perfil publicador."
                ));

        EventoInteraccion evento = new EventoInteraccion();
        evento.setPerfilPublicadorId(perfil.getId());
        evento.setTipo(tipo);
        evento.setCreatedAt(OffsetDateTime.now());

        eventoInteraccionRepository.save(evento);
    }

    /**
     * Conteos de los últimos 30 días de un PERFIL, por tipo. Alimenta
     * "contactos desde tu perfil" en el panel, separado de los
     * contactos de cada actividad.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> contarUltimos30DiasDePerfil(Long perfilPublicadorId) {
        Map<String, Long> conteos = new HashMap<>();

        if (perfilPublicadorId == null) {
            return conteos;
        }

        OffsetDateTime desde = OffsetDateTime.now().minusDays(DIAS_VENTANA_METRICAS);

        for (Object[] fila : eventoInteraccionRepository
                .contarPorPerfilYTipo(perfilPublicadorId, desde)) {
            conteos.put((String) fila[0], (Long) fila[1]);
        }

        return conteos;
    }

    /**
     * Deportes más vistos en una ventana (Fase 6): reemplaza a la
     * sección "populares" de la home, que hasta ahora era una lista
     * HARDCODEADA de seis deportes sin ninguna métrica detrás.
     *
     * Se agrupa por deporte a partir del ranking de actividades: el
     * tracking cuelga de la actividad, no del deporte.
     *
     * Devuelve una lista ordenada de [deporteSlug, deporteNombre,
     * vistas]. Si no hay señal suficiente devuelve vacío — y el
     * frontend no dibuja la sección. Nunca un ranking inventado.
     */
    @Transactional(readOnly = true)
    public List<Object[]> deportesMasVistos(int dias, int minimoDeportes, int limite) {
        int ventana = Math.min(Math.max(dias, 1), 365);
        OffsetDateTime desde = OffsetDateTime.now().minusDays(ventana);

        /*
          Se pide un techo generoso de actividades porque varias pueden
          ser del mismo deporte: el ranking final es por deporte.
        */
        List<Object[]> porActividad = eventoInteraccionRepository.rankingDeActividades(
                "VISTA_DETALLE",
                desde,
                org.springframework.data.domain.PageRequest.of(0, 200)
        );

        if (porActividad.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> vistasPorActividad = new HashMap<>();
        for (Object[] fila : porActividad) {
            vistasPorActividad.put((Long) fila[0], ((Number) fila[1]).longValue());
        }

        /* Un solo findAllById: el deporte sale de la actividad. */
        Map<String, long[]> vistasPorDeporte = new HashMap<>();
        Map<String, String> nombrePorSlug = new HashMap<>();

        for (Actividad actividad
                : actividadRepository.findAllById(vistasPorActividad.keySet())) {
            if (actividad.getDeporte() == null
                    || !Boolean.TRUE.equals(actividad.getActiva())
                    || actividad.getDeletedAt() != null) {
                continue;
            }

            String slug = actividad.getDeporte().getSlug();
            if (slug == null) {
                continue;
            }

            nombrePorSlug.putIfAbsent(slug, actividad.getDeporte().getNombre());
            vistasPorDeporte
                    .computeIfAbsent(slug, clave -> new long[1])[0] +=
                    vistasPorActividad.getOrDefault(actividad.getId(), 0L);
        }

        /*
          Con menos de N deportes con señal, el "ranking" lo arman dos
          clicks y miente: mejor no mostrar nada.
        */
        if (vistasPorDeporte.size() < Math.max(minimoDeportes, 1)) {
            return List.of();
        }

        return vistasPorDeporte.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(Math.min(Math.max(limite, 1), 20))
                .map(entrada -> new Object[]{
                        entrada.getKey(),
                        nombrePorSlug.get(entrada.getKey()),
                        entrada.getValue()[0]
                })
                .toList();
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
