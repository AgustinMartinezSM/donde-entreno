package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PublicadorMetricasDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Métricas de resumen del panel del publicador autenticado.
 *
 * Todos los conteos se acotan al perfil del publicador que consulta
 * (resuelto desde el userId del JWT), así que no exponen datos ajenos.
 * Es solo lectura: no muta nada.
 */
@Service
public class PublicadorMetricasService {

    private static final String ESTADO_ACTIVIDAD_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_ACTIVIDAD_PAUSADA = "PAUSADA";
    private static final String ESTADO_SOLICITUD_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_MODERACION_PENDIENTE = "PENDIENTE";
    private static final List<String> ESTADOS_CAMBIO_ABIERTOS =
            List.of("PENDIENTE", "EN_REVISION");
    private static final String MENSAJE_PERFIL_NO_ENCONTRADO = "Perfil publicador no encontrado.";

    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final ActividadRepository actividadRepository;
    private final SolicitudPublicacionRepository solicitudPublicacionRepository;
    private final SolicitudCambioActividadRepository solicitudCambioActividadRepository;
    private final ImagenRepository imagenRepository;
    private final SeguimientoPublicadorRepository seguimientoPublicadorRepository;
    private final InteraccionService interaccionService;
    private final com.dondeentreno.api.repository.InteresActividadRepository interesActividadRepository;

    public PublicadorMetricasService(
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            SolicitudPublicacionRepository solicitudPublicacionRepository,
            SolicitudCambioActividadRepository solicitudCambioActividadRepository,
            ImagenRepository imagenRepository,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository,
            InteraccionService interaccionService,
            com.dondeentreno.api.repository.InteresActividadRepository interesActividadRepository
    ) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
        this.solicitudCambioActividadRepository = solicitudCambioActividadRepository;
        this.imagenRepository = imagenRepository;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
        this.interaccionService = interaccionService;
        this.interesActividadRepository = interesActividadRepository;
    }

    @Transactional(readOnly = true)
    public PublicadorMetricasDTO obtenerMetricas(Long userId) {
        PerfilPublicador perfil = obtenerPerfilPublicador(userId);
        Long perfilId = perfil.getId();

        long actividadesPublicadas = actividadRepository
                .countByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        perfilId,
                        ESTADO_ACTIVIDAD_PUBLICADA
                );

        /* Pausa voluntaria (fase 6): el dashboard las muestra aparte. */
        long actividadesPausadas = actividadRepository
                .countByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        perfilId,
                        ESTADO_ACTIVIDAD_PAUSADA
                );

        long solicitudesPublicacionPendientes = solicitudPublicacionRepository
                .countByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
                        userId,
                        perfilId,
                        ESTADO_SOLICITUD_PENDIENTE
                );

        long solicitudesCambioPendientes = solicitudCambioActividadRepository
                .countByPerfilPublicador_IdAndEstadoInAndDeletedAtIsNull(
                        perfilId,
                        ESTADOS_CAMBIO_ABIERTOS
                );

        long imagenesPendientesModeracion = imagenRepository
                .countByEstadoModeracionAndActividad_PerfilPublicador_Id(
                        ESTADO_MODERACION_PENDIENTE,
                        perfilId
                );

        long seguidores = seguimientoPublicadorRepository
                .countByPerfilPublicador_Id(perfilId);

        /*
          Interacciones de los últimos 30 días (Fase 2 social): un solo
          query agrupado sobre todas las actividades del perfil.
        */
        List<Long> actividadIds = actividadRepository.idsDePerfil(perfilId);
        var conteosInteracciones = interaccionService.contarUltimos30Dias(actividadIds);
        long vistas30Dias = sumarTipo(conteosInteracciones, "VISTA_DETALLE");
        long contactosWhatsapp30Dias = sumarTipo(conteosInteracciones, "CLICK_WHATSAPP");

        /* Fase 3: interés agregado sobre todas sus actividades. */
        long quierenProbar = actividadIds.isEmpty()
                ? 0
                : interesActividadRepository.countByActividadIdInAndEstado(
                        actividadIds,
                        InteresActividadService.QUIERO_PROBAR
                );

        return new PublicadorMetricasDTO(
                actividadesPublicadas,
                actividadesPausadas,
                solicitudesPublicacionPendientes,
                solicitudesCambioPendientes,
                imagenesPendientesModeracion,
                seguidores,
                vistas30Dias,
                contactosWhatsapp30Dias,
                quierenProbar
        );
    }

    private long sumarTipo(
            java.util.Map<Long, java.util.Map<String, Long>> conteos,
            String tipo
    ) {
        return conteos.values().stream()
                .mapToLong(porTipo -> porTipo.getOrDefault(tipo, 0L))
                .sum();
    }

    private PerfilPublicador obtenerPerfilPublicador(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        return perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PERFIL_NO_ENCONTRADO));
    }
}
