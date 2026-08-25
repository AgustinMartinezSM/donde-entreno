package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.EventoDeportivo;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.EventoDeportivoRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.InteresEventoRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
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
 * Eventos deportivos (script 35, Fase 9): las reglas que no se ven en
 * el schema — la fecha pasada, el tope de campanita, cancelar ≠ borrar
 * y la sede ajena.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoDeportivoServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long PERFIL_ID = 8L;
    private static final Long UBICACION_ID = 55L;
    private static final Long DEPORTE_ID = 3L;

    @Mock
    private EventoDeportivoRepository eventoRepository;

    @Mock
    private InteresEventoRepository interesEventoRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private DeporteRepository deporteRepository;

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
    private EventoSlugService eventoSlugService;

    private EventoDeportivoService service;

    @BeforeEach
    void setUp() {
        service = new EventoDeportivoService(
                eventoRepository,
                interesEventoRepository,
                perfilPublicadorRepository,
                actividadRepository,
                ubicacionRepository,
                deporteRepository,
                imagenRepository,
                imagenService,
                seguimientoPublicadorRepository,
                notificacionService,
                feedEventService,
                eventoSlugService
        );

        when(perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(USUARIO_ID))
                .thenReturn(Optional.of(perfil()));
        when(ubicacionRepository.findById(UBICACION_ID)).thenReturn(Optional.of(ubicacion(PERFIL_ID)));
        when(deporteRepository.findById(DEPORTE_ID)).thenReturn(Optional.of(deporte()));
        when(eventoSlugService.generarSlugUnico(anyString())).thenReturn("torneo-de-verano");
        when(eventoRepository.saveAndFlush(any(EventoDeportivo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(imagenService.obtenerLogosAprobadosPorPerfil(anyList())).thenReturn(Map.of());
        when(ubicacionRepository.findAllById(anyList())).thenReturn(List.of(ubicacion(PERFIL_ID)));
        when(deporteRepository.findAllById(anyList())).thenReturn(List.of(deporte()));
        when(perfilPublicadorRepository.findAllById(anyList())).thenReturn(List.of(perfil()));
        when(interesEventoRepository.contarPorEventos(anyList())).thenReturn(List.of());
        when(seguimientoPublicadorRepository.usuarioIdsSeguidoresDe(PERFIL_ID))
                .thenReturn(List.of(101L, 102L));
    }

    @Test
    void publicarGuardaPublicadoYAvisaALosSeguidores() {
        when(eventoRepository.countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                eq(PERFIL_ID), any(OffsetDateTime.class))).thenReturn(1L);

        service.publicar(USUARIO_ID, datos(OffsetDateTime.now().plusDays(5)));

        ArgumentCaptor<EventoDeportivo> guardado = ArgumentCaptor.forClass(EventoDeportivo.class);
        verify(eventoRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getEstado()).isEqualTo("PUBLICADO");
        assertThat(guardado.getValue().getSlug()).isEqualTo("torneo-de-verano");

        verify(feedEventService).emitirEvento(eq(PERFIL_ID), any(), any(), anyString());
        verify(notificacionService).emitirATodos(
                eq(List.of(101L, 102L)), eq("EVENTO_NUEVO"), anyString(), anyString());
    }

    /**
     * Cargar la agenda del mes de una sentada es legítimo; disparar
     * cientos de campanitas a la misma gente, no. Del tercero en
     * adelante se publica igual pero sin aviso.
     */
    @Test
    void pasadoElTopeDiarioSePublicaIgualPeroSinCampanita() {
        when(eventoRepository.countByPerfilPublicadorIdAndCreatedAtGreaterThanEqual(
                eq(PERFIL_ID), any(OffsetDateTime.class))).thenReturn(3L);

        service.publicar(USUARIO_ID, datos(OffsetDateTime.now().plusDays(5)));

        verify(eventoRepository).saveAndFlush(any(EventoDeportivo.class));
        verify(feedEventService).emitirEvento(eq(PERFIL_ID), any(), any(), anyString());
        verify(notificacionService, never())
                .emitirATodos(anyList(), anyString(), anyString(), anyString());
    }

    /**
     * Un evento con fecha pasada no es un dato histórico, es un error
     * de carga (el clásico: el año equivocado).
     */
    @Test
    void unEventoConFechaPasadaNoSeCrea() {
        assertThatThrownBy(() ->
                service.publicar(USUARIO_ID, datos(OffsetDateTime.now().minusDays(1))))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(eventoRepository, never()).saveAndFlush(any(EventoDeportivo.class));
    }

    @Test
    void unEventoQueTerminaAntesDeEmpezarNoSeCrea() {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(5);

        assertThatThrownBy(() -> service.publicar(USUARIO_ID,
                new EventoDeportivoService.DatosEvento(
                        "Torneo", "Descripción", inicio, inicio.minusHours(2),
                        null, UBICACION_ID, DEPORTE_ID, null, null, false, null, true)))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(eventoRepository, never()).saveAndFlush(any(EventoDeportivo.class));
    }

    /**
     * Acá SÍ se falla (a diferencia de la foto de la novedad): de la
     * sede salen la ciudad del calendario y el "cómo llegar", así que
     * aceptar una ajena en silencio dejaría el evento en otro lado.
     */
    @Test
    void conUnaSedeAjenaNoSePublica() {
        when(ubicacionRepository.findById(UBICACION_ID))
                .thenReturn(Optional.of(ubicacion(999L)));

        assertThatThrownBy(() ->
                service.publicar(USUARIO_ID, datos(OffsetDateTime.now().plusDays(5))))
                .isInstanceOf(FiltroInvalidoException.class);

        verify(eventoRepository, never()).saveAndFlush(any(EventoDeportivo.class));
    }

    @Test
    void sinSedeNiActividadNoSePublica() {
        assertThatThrownBy(() -> service.publicar(USUARIO_ID,
                new EventoDeportivoService.DatosEvento(
                        "Torneo", "Descripción", OffsetDateTime.now().plusDays(5), null,
                        null, null, DEPORTE_ID, null, null, false, null, true)))
                .isInstanceOf(FiltroInvalidoException.class);
    }

    /**
     * Cancelar y borrar NO son lo mismo: el link del evento ya circuló
     * por WhatsApp, así que el cancelado sigue existiendo para decir
     * que se canceló.
     */
    @Test
    void cancelarDejaElEventoVivoYReportable() {
        EventoDeportivo evento = eventoPropio();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        service.cancelar(USUARIO_ID, 5L);

        assertThat(evento.getEstado()).isEqualTo("CANCELADO");
        assertThat(service.esVisible(5L)).isTrue();
    }

    @Test
    void borrarLoSacaDeTodasLasVistas() {
        EventoDeportivo evento = eventoPropio();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        service.eliminarPropio(USUARIO_ID, 5L);

        assertThat(evento.getEstado()).isEqualTo("ELIMINADO_POR_PUBLICADOR");
        assertThat(service.esVisible(5L)).isFalse();
    }

    @Test
    void ocultarPorAdminLoSacaDeCirculacion() {
        EventoDeportivo evento = eventoPropio();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThat(service.esVisible(5L)).isTrue();

        service.ocultarPorAdmin(5L);

        assertThat(evento.getEstado()).isEqualTo("OCULTO_POR_ADMIN");
        assertThat(service.esVisible(5L)).isFalse();
    }

    /** 404 y no 403: no se delata que el evento ajeno existe. */
    @Test
    void cancelarUnEventoAjenoDa404YNoLoToca() {
        EventoDeportivo ajeno = eventoPropio();
        ajeno.setPerfilPublicadorId(999L);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> service.cancelar(USUARIO_ID, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        assertThat(ajeno.getEstado()).isEqualTo("PUBLICADO");
        verify(eventoRepository, never()).save(any(EventoDeportivo.class));
    }

    /** El UNIQUE lo hace idempotente: marcar dos veces no duplica. */
    @Test
    void marcarInteresDosVecesNoDuplica() {
        EventoDeportivo evento = eventoPropio();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));
        when(interesEventoRepository.existsByUsuarioIdAndEventoDeportivoId(USUARIO_ID, 5L))
                .thenReturn(false, true);
        when(interesEventoRepository.countByEventoDeportivoId(5L)).thenReturn(1L);

        assertThat(service.marcarInteres(USUARIO_ID, 5L)).isEqualTo(1L);
        assertThat(service.marcarInteres(USUARIO_ID, 5L)).isEqualTo(1L);

        verify(interesEventoRepository, org.mockito.Mockito.times(1))
                .saveAndFlush(any(com.dondeentreno.api.entity.InteresEvento.class));
    }

    /** No se puede marcar interés en algo que ya no está publicado. */
    @Test
    void noSePuedeMarcarInteresEnUnEventoOculto() {
        EventoDeportivo evento = eventoPropio();
        evento.setEstado("OCULTO_POR_ADMIN");
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.marcarInteres(USUARIO_ID, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private EventoDeportivoService.DatosEvento datos(OffsetDateTime inicia) {
        return new EventoDeportivoService.DatosEvento(
                "Torneo de verano",
                "Un torneo abierto para todos los niveles.",
                inicia,
                null,
                null,
                UBICACION_ID,
                DEPORTE_ID,
                null,
                null,
                false,
                null,
                true
        );
    }

    private EventoDeportivo eventoPropio() {
        EventoDeportivo evento = new EventoDeportivo();
        evento.setPerfilPublicadorId(PERFIL_ID);
        evento.setEstado("PUBLICADO");
        evento.setTitulo("Torneo de verano");
        evento.setSlug("torneo-de-verano");
        evento.setIniciaAt(OffsetDateTime.now().plusDays(3));
        return evento;
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(PERFIL_ID);
        perfil.setNombre("Club Atlético Sur");
        perfil.setSlug("club-atletico-sur");
        return perfil;
    }

    private Ubicacion ubicacion(Long perfilId) {
        PerfilPublicador duenio = new PerfilPublicador();
        duenio.setId(perfilId);

        Ciudad ciudad = new Ciudad();
        ciudad.setId(1L);
        ciudad.setNombre("Mar del Plata");
        ciudad.setSlug("mar-del-plata");

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(UBICACION_ID);
        ubicacion.setPerfilPublicador(duenio);
        ubicacion.setCiudad(ciudad);
        ubicacion.setNombre("Sede central");
        ubicacion.setDireccion("Calle falsa 123");
        ubicacion.setActiva(true);
        return ubicacion;
    }

    private Deporte deporte() {
        Deporte deporte = new Deporte();
        deporte.setId(DEPORTE_ID);
        deporte.setNombre("Karate");
        deporte.setSlug("karate");
        deporte.setActivo(true);
        return deporte;
    }
}
