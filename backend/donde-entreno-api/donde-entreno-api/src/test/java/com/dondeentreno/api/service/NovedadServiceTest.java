package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Novedad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.NovedadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Canal de novedades (script 34, Fase 8): tope diario, campanita solo
 * en la primera del día y las dos bajas (publicador y admin).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NovedadServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long PERFIL_ID = 8L;

    @Mock
    private NovedadRepository novedadRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private ImagenService imagenService;

    @Mock
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private FeedEventService feedEventService;

    @Mock
    private com.dondeentreno.api.repository.MeGustaNovedadRepository meGustaNovedadRepository;

    private NovedadService service;

    @BeforeEach
    void setUp() {
        service = new NovedadService(
                novedadRepository,
                perfilPublicadorRepository,
                imagenRepository,
                imagenService,
                seguimientoPublicadorRepository,
                notificacionService,
                feedEventService,
                meGustaNovedadRepository
        );

        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(USUARIO_ID))
                .thenReturn(Optional.of(perfil()));
        when(novedadRepository.saveAndFlush(any(Novedad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(imagenService.obtenerLogosAprobadosPorPerfil(anyList()))
                .thenReturn(Map.of());
        when(seguimientoPublicadorRepository.usuarioIdsSeguidoresDe(PERFIL_ID))
                .thenReturn(List.of(101L, 102L));
    }

    @Test
    void laPrimeraDelDiaSeGuardaVisibleVaAlFeedYAvisaALosSeguidores() {
        when(novedadRepository.countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                eq(PERFIL_ID), any(OffsetDateTime.class))).thenReturn(0L);

        service.publicar(USUARIO_ID, "  Cambiamos el horario del sábado  ", null);

        ArgumentCaptor<Novedad> guardada = ArgumentCaptor.forClass(Novedad.class);
        verify(novedadRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo("VISIBLE");
        /* El trim es el que se persiste, no el texto crudo. */
        assertThat(guardada.getValue().getTexto()).isEqualTo("Cambiamos el horario del sábado");

        verify(feedEventService).emitirNovedad(eq(PERFIL_ID), any(), eq(null), anyString());
        verify(notificacionService).emitirATodos(
                eq(List.of(101L, 102L)), eq("NOVEDAD_PUBLICADOR"), anyString(), anyString());
    }

    /**
     * El contrato del canal: la segunda del día entra al feed pero NO
     * dispara campanita. NotificacionService no agrupa, así que tres
     * novedades por 50 seguidores serían 150 avisos a la misma gente.
     */
    @Test
    void laSegundaDelDiaEntraAlFeedPeroNoVuelveANotificar() {
        when(novedadRepository.countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                eq(PERFIL_ID), any(OffsetDateTime.class))).thenReturn(1L);

        service.publicar(USUARIO_ID, "Quedan 3 lugares", null);

        verify(feedEventService).emitirNovedad(eq(PERFIL_ID), any(), eq(null), anyString());
        verify(notificacionService, never())
                .emitirATodos(anyList(), anyString(), anyString(), anyString());
    }

    @Test
    void pasadoElTopeDiarioNoSeGuardaNada() {
        when(novedadRepository.countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                eq(PERFIL_ID), any(OffsetDateTime.class))).thenReturn(3L);

        assertThatThrownBy(() -> service.publicar(USUARIO_ID, "La cuarta del día", null))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(novedadRepository, never()).saveAndFlush(any(Novedad.class));
        verify(feedEventService, never())
                .emitirNovedad(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void unaNovedadVaciaNoSePublica() {
        assertThatThrownBy(() -> service.publicar(USUARIO_ID, "   ", null))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(novedadRepository, never()).saveAndFlush(any(Novedad.class));
    }

    /**
     * Una foto que no es suya no puede voltear la publicación: la
     * novedad sale SIN foto, porque el texto es lo que importa.
     */
    @Test
    void conUnaFotoAjenaLaNovedadSePublicaSinFoto() {
        when(novedadRepository.countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                eq(PERFIL_ID), any(OffsetDateTime.class))).thenReturn(0L);
        when(imagenRepository.findById(77L)).thenReturn(Optional.empty());

        service.publicar(USUARIO_ID, "Con foto prestada", 77L);

        ArgumentCaptor<Novedad> guardada = ArgumentCaptor.forClass(Novedad.class);
        verify(novedadRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getImagenId()).isNull();
    }

    /**
     * 404 y no 403: contestar "prohibido" delataría que la novedad
     * existe (mismo patrón que los likes).
     */
    @Test
    void borrarUnaNovedadAjenaDa404YNoLaToca() {
        Novedad ajena = new Novedad();
        ajena.setPerfilPublicadorId(99L);
        ajena.setEstado("VISIBLE");
        when(novedadRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.eliminarPropia(USUARIO_ID, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        assertThat(ajena.getEstado()).isEqualTo("VISIBLE");
        verify(novedadRepository, never()).save(any(Novedad.class));
    }

    @Test
    void borrarLaPropiaLaDejaEnBajaLogica() {
        Novedad propia = new Novedad();
        propia.setPerfilPublicadorId(PERFIL_ID);
        propia.setEstado("VISIBLE");
        when(novedadRepository.findById(5L)).thenReturn(Optional.of(propia));

        service.eliminarPropia(USUARIO_ID, 5L);

        assertThat(propia.getEstado()).isEqualTo("ELIMINADA_POR_PUBLICADOR");
        verify(novedadRepository).save(propia);
    }

    @Test
    void ocultarPorAdminLaSacaDeCirculacionYDejaDeSerReportable() {
        Novedad novedad = new Novedad();
        novedad.setPerfilPublicadorId(PERFIL_ID);
        novedad.setEstado("VISIBLE");
        when(novedadRepository.findById(5L)).thenReturn(Optional.of(novedad));

        assertThat(service.esVisible(5L)).isTrue();

        service.ocultarPorAdmin(5L);

        assertThat(novedad.getEstado()).isEqualTo("OCULTA_POR_ADMIN");
        assertThat(service.esVisible(5L)).isFalse();
    }

    /* ===================== reacciones (script 37) ===================== */

    /** El UNIQUE lo hace idempotente: reaccionar dos veces no suma dos. */
    @Test
    void reaccionarDosVecesNoDuplica() {
        Novedad visible = new Novedad();
        visible.setPerfilPublicadorId(PERFIL_ID);
        visible.setEstado("VISIBLE");
        when(novedadRepository.findById(5L)).thenReturn(Optional.of(visible));
        when(meGustaNovedadRepository.existsByUsuarioIdAndNovedadId(USUARIO_ID, 5L))
                .thenReturn(false, true);
        when(meGustaNovedadRepository.countByNovedadId(5L)).thenReturn(1L);

        assertThat(service.darMeGusta(USUARIO_ID, 5L)).isEqualTo(1L);
        assertThat(service.darMeGusta(USUARIO_ID, 5L)).isEqualTo(1L);

        verify(meGustaNovedadRepository, org.mockito.Mockito.times(1))
                .saveAndFlush(any(com.dondeentreno.api.entity.MeGustaNovedad.class));
    }

    /**
     * Una reacción NO notifica al publicador: veinte "me gusta" serían
     * veinte campanitas por algo que no pide respuesta (mismo criterio
     * que los likes de fotos).
     */
    @Test
    void reaccionarNoNotificaAlPublicador() {
        Novedad visible = new Novedad();
        visible.setPerfilPublicadorId(PERFIL_ID);
        visible.setEstado("VISIBLE");
        when(novedadRepository.findById(5L)).thenReturn(Optional.of(visible));

        service.darMeGusta(USUARIO_ID, 5L);

        verify(notificacionService, never())
                .emitir(anyLong(), anyString(), anyString(), anyString());
        verify(notificacionService, never())
                .emitirATodos(anyList(), anyString(), anyString(), anyString());
    }

    /** Una novedad ocultada por el admin no acepta reacciones. */
    @Test
    void noSePuedeReaccionarAUnaNovedadOculta() {
        Novedad oculta = new Novedad();
        oculta.setPerfilPublicadorId(PERFIL_ID);
        oculta.setEstado("OCULTA_POR_ADMIN");
        when(novedadRepository.findById(5L)).thenReturn(Optional.of(oculta));

        assertThatThrownBy(() -> service.darMeGusta(USUARIO_ID, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(meGustaNovedadRepository, never())
                .saveAndFlush(any(com.dondeentreno.api.entity.MeGustaNovedad.class));
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(PERFIL_ID);
        perfil.setNombre("Club Atlético Sur");
        perfil.setSlug("club-atletico-sur");
        return perfil;
    }
}
