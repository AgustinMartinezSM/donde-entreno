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

    public PublicadorMetricasService(
            PerfilPublicadorRepository perfilPublicadorRepository,
            ActividadRepository actividadRepository,
            SolicitudPublicacionRepository solicitudPublicacionRepository,
            SolicitudCambioActividadRepository solicitudCambioActividadRepository,
            ImagenRepository imagenRepository,
            SeguimientoPublicadorRepository seguimientoPublicadorRepository
    ) {
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.actividadRepository = actividadRepository;
        this.solicitudPublicacionRepository = solicitudPublicacionRepository;
        this.solicitudCambioActividadRepository = solicitudCambioActividadRepository;
        this.imagenRepository = imagenRepository;
        this.seguimientoPublicadorRepository = seguimientoPublicadorRepository;
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

        return new PublicadorMetricasDTO(
                actividadesPublicadas,
                solicitudesPublicacionPendientes,
                solicitudesCambioPendientes,
                imagenesPendientesModeracion,
                seguidores
        );
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
