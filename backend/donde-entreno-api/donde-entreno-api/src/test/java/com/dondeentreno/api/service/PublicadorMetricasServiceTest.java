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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicadorMetricasServiceTest {

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Mock
    private SolicitudCambioActividadRepository solicitudCambioActividadRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    private PublicadorMetricasService publicadorMetricasService;

    @BeforeEach
    void setUp() {
        publicadorMetricasService = new PublicadorMetricasService(
                perfilPublicadorRepository,
                actividadRepository,
                solicitudPublicacionRepository,
                solicitudCambioActividadRepository,
                imagenRepository,
                seguimientoPublicadorRepository
        );
    }

    @Test
    void obtenerMetricasDevuelveLosConteosDelPerfilAutenticado() {
        Long userId = 55L;
        Long perfilId = 10L;
        PerfilPublicador perfil = mock(PerfilPublicador.class);
        when(perfil.getId()).thenReturn(perfilId);
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(perfil));

        when(actividadRepository
                .countByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(perfilId, "PUBLICADA"))
                .thenReturn(3L);
        when(solicitudPublicacionRepository
                .countByUsuario_IdAndPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(userId, perfilId, "PENDIENTE"))
                .thenReturn(2L);
        when(solicitudCambioActividadRepository
                .countByPerfilPublicador_IdAndEstadoInAndDeletedAtIsNull(
                        eq(perfilId), eq(List.of("PENDIENTE", "EN_REVISION"))))
                .thenReturn(1L);
        when(imagenRepository
                .countByEstadoModeracionAndActividad_PerfilPublicador_Id("PENDIENTE", perfilId))
                .thenReturn(4L);
        when(seguimientoPublicadorRepository.countByPerfilPublicador_Id(perfilId))
                .thenReturn(6L);

        PublicadorMetricasDTO metricas = publicadorMetricasService.obtenerMetricas(userId);

        assertEquals(3L, metricas.getActividadesPublicadas());
        assertEquals(2L, metricas.getSolicitudesPublicacionPendientes());
        assertEquals(1L, metricas.getSolicitudesCambioPendientes());
        assertEquals(4L, metricas.getImagenesPendientesModeracion());
        assertEquals(6L, metricas.getSeguidores());

        verify(actividadRepository)
                .countByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(perfilId, "PUBLICADA");
        verify(solicitudCambioActividadRepository)
                .countByPerfilPublicador_IdAndEstadoInAndDeletedAtIsNull(
                        eq(perfilId), eq(List.of("PENDIENTE", "EN_REVISION")));
    }

    @Test
    void obtenerMetricasSinPerfilLanzaRecursoNoEncontrado() {
        Long userId = 77L;
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> publicadorMetricasService.obtenerMetricas(userId)
        );

        verifyNoInteractions(actividadRepository);
        verifyNoInteractions(solicitudPublicacionRepository);
        verifyNoInteractions(solicitudCambioActividadRepository);
        verifyNoInteractions(imagenRepository);
        verifyNoInteractions(seguimientoPublicadorRepository);
    }

    @Test
    void obtenerMetricasConUserIdNuloLanzaCredencialesInvalidas() {
        assertThrows(
                CredencialesInvalidasException.class,
                () -> publicadorMetricasService.obtenerMetricas(null)
        );

        verifyNoInteractions(perfilPublicadorRepository);
        verifyNoInteractions(actividadRepository);
    }
}
