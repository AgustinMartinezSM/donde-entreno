package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.EventoInteraccion;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EventoInteraccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteraccionServiceTest {

    @Mock
    private EventoInteraccionRepository eventoInteraccionRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private com.dondeentreno.api.repository.PerfilPublicadorRepository perfilPublicadorRepository;

    private InteraccionService service;

    @BeforeEach
    void setUp() {
        service = new InteraccionService(
                eventoInteraccionRepository,
                actividadRepository,
                perfilPublicadorRepository
        );
    }

    @Test
    void registrarGuardaElEventoSinUsuario() {
        when(actividadRepository.findById(70L)).thenReturn(Optional.of(actividadPublica()));

        service.registrar(70L, "CLICK_WHATSAPP");

        ArgumentCaptor<EventoInteraccion> captor =
                ArgumentCaptor.forClass(EventoInteraccion.class);
        verify(eventoInteraccionRepository).save(captor.capture());
        assertEquals(70L, captor.getValue().getActividadId());
        assertEquals("CLICK_WHATSAPP", captor.getValue().getTipo());
    }

    @Test
    void tipoFueraDeCatalogoDa400() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.registrar(70L, "CLICK_RARO")
        );
    }

    @Test
    void actividadNoPublicaDa404() {
        Actividad pausada = actividadPublica();
        pausada.setEstadoPublicacion("PAUSADA");
        when(actividadRepository.findById(70L)).thenReturn(Optional.of(pausada));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.registrar(70L, "VISTA_DETALLE")
        );
    }

    @Test
    void contarUltimos30DiasAgrupaPorActividadYTipo() {
        when(eventoInteraccionRepository.contarPorActividadYTipo(
                anyCollection(), any(OffsetDateTime.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{70L, "VISTA_DETALLE", 12L},
                        new Object[]{70L, "CLICK_WHATSAPP", 3L},
                        new Object[]{71L, "VISTA_DETALLE", 5L}
                ));

        Map<Long, Map<String, Long>> conteos =
                service.contarUltimos30Dias(List.of(70L, 71L));

        assertEquals(12L, conteos.get(70L).get("VISTA_DETALLE"));
        assertEquals(3L, conteos.get(70L).get("CLICK_WHATSAPP"));
        assertEquals(5L, conteos.get(71L).get("VISTA_DETALLE"));
    }

    @Test
    void sinActividadesNoConsultaLaBase() {
        assertTrue(service.contarUltimos30Dias(List.of()).isEmpty());
    }

    /* ============ Ranking de deportes (Fase 6) ============ */

    /**
     * El ranking agrupa POR DEPORTE a partir de las actividades: dos
     * actividades del mismo deporte suman al mismo casillero.
     */
    @Test
    void elRankingAgrupaVistasPorDeporte() {
        when(eventoInteraccionRepository.rankingDeActividades(
                org.mockito.ArgumentMatchers.eq("VISTA_DETALLE"),
                any(),
                any()
        )).thenReturn(List.<Object[]>of(
                new Object[]{70L, 10L},
                new Object[]{71L, 5L},
                new Object[]{72L, 4L}
        ));
        when(actividadRepository.findAllById(anyCollection())).thenReturn(List.of(
                actividadDeDeporte(70L, "karate", "Karate"),
                actividadDeDeporte(71L, "karate", "Karate"),
                actividadDeDeporte(72L, "yoga", "Yoga")
        ));

        List<Object[]> ranking = service.deportesMasVistos(30, 2, 6);

        assertEquals(2, ranking.size());
        /* karate = 10 + 5 = 15, primero; yoga = 4. */
        assertEquals("karate", ranking.get(0)[0]);
        assertEquals(15L, ranking.get(0)[2]);
        assertEquals("yoga", ranking.get(1)[0]);
    }

    /**
     * Con menos deportes que el mínimo, el ranking NO se publica: con
     * este volumen de tráfico lo armarían dos clicks.
     */
    @Test
    void conPocaSenalElRankingVieneVacio() {
        when(eventoInteraccionRepository.rankingDeActividades(
                org.mockito.ArgumentMatchers.eq("VISTA_DETALLE"),
                any(),
                any()
        )).thenReturn(List.<Object[]>of(new Object[]{70L, 3L}));
        when(actividadRepository.findAllById(anyCollection()))
                .thenReturn(List.of(actividadDeDeporte(70L, "karate", "Karate")));

        assertTrue(service.deportesMasVistos(30, 3, 6).isEmpty());
    }

    @Test
    void sinEventosNoConsultaActividades() {
        when(eventoInteraccionRepository.rankingDeActividades(
                org.mockito.ArgumentMatchers.eq("VISTA_DETALLE"),
                any(),
                any()
        )).thenReturn(List.of());

        assertTrue(service.deportesMasVistos(30, 3, 6).isEmpty());
        verify(actividadRepository, org.mockito.Mockito.never()).findAllById(anyCollection());
    }

    /** Una actividad dada de baja no ensucia el ranking. */
    @Test
    void lasActividadesNoVivasSeIgnoran() {
        when(eventoInteraccionRepository.rankingDeActividades(
                org.mockito.ArgumentMatchers.eq("VISTA_DETALLE"),
                any(),
                any()
        )).thenReturn(List.<Object[]>of(new Object[]{70L, 99L}));

        Actividad borrada = actividadDeDeporte(70L, "karate", "Karate");
        borrada.setDeletedAt(OffsetDateTime.now());
        when(actividadRepository.findAllById(anyCollection())).thenReturn(List.of(borrada));

        assertTrue(service.deportesMasVistos(30, 1, 6).isEmpty());
    }

    private Actividad actividadDeDeporte(Long id, String slug, String nombre) {
        Actividad actividad = new Actividad();
        try {
            java.lang.reflect.Field campo = Actividad.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(actividad, id);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");

        com.dondeentreno.api.entity.Deporte deporte = new com.dondeentreno.api.entity.Deporte();
        deporte.setSlug(slug);
        deporte.setNombre(nombre);
        actividad.setDeporte(deporte);

        return actividad;
    }

    private Actividad actividadPublica() {
        Actividad actividad = new Actividad();
        try {
            java.lang.reflect.Field campo = Actividad.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(actividad, 70L);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        return actividad;
    }

    /**
     * El umbral del ranking (Fase 10): con menos actividades con señal
     * que el mínimo, la lista sale VACÍA.
     *
     * Va como unit test y no como IT a propósito: el umbral cuenta
     * sobre TODA la base, así que un IT lo daría verde o rojo según los
     * datos ajenos que hubiera en la base local.
     */
    @Test
    void elRankingSeApagaSinSenalSuficiente() {
        when(eventoInteraccionRepository.rankingDeActividades(
                eq("VISTA_DETALLE"), any(), any()
        )).thenReturn(List.<Object[]>of(new Object[]{70L, 30L}));

        when(actividadRepository.findAllById(any()))
                .thenReturn(List.of(actividadPublica()));

        /* Una sola con señal contra un mínimo de tres. */
        assertTrue(service.actividadesMasVistas(7, 3, 6).isEmpty());
    }

    /**
     * Con señal suficiente sí devuelve, y respetando el orden del
     * ranking: `findAllById` no garantiza el orden de los ids.
     */
    @Test
    void elRankingRespetaElOrdenDelTrackingYNoElDeLaBase() {
        when(eventoInteraccionRepository.rankingDeActividades(
                eq("VISTA_DETALLE"), any(), any()
        )).thenReturn(List.<Object[]>of(
                new Object[]{72L, 30L},
                new Object[]{70L, 20L},
                new Object[]{71L, 10L}
        ));

        /* A propósito en otro orden que el ranking. */
        when(actividadRepository.findAllById(any())).thenReturn(List.of(
                actividadPublicaConId(70L),
                actividadPublicaConId(71L),
                actividadPublicaConId(72L)
        ));

        List<Actividad> ranking = service.actividadesMasVistas(7, 3, 6);

        assertEquals(3, ranking.size());
        assertEquals(72L, ranking.get(0).getId());
        assertEquals(70L, ranking.get(1).getId());
        assertEquals(71L, ranking.get(2).getId());
    }

    /**
     * Una despublicada con muchas vistas no entra, y además NO cuenta
     * para el umbral: si contara, dos publicadas más una despublicada
     * alcanzarían el mínimo de tres y la sección se dibujaría con dos.
     */
    @Test
    void unaDespublicadaNoCuentaNiSiquieraParaElUmbral() {
        when(eventoInteraccionRepository.rankingDeActividades(
                eq("VISTA_DETALLE"), any(), any()
        )).thenReturn(List.<Object[]>of(
                new Object[]{70L, 30L},
                new Object[]{71L, 20L},
                new Object[]{72L, 10L}
        ));

        Actividad despublicada = actividadPublicaConId(72L);
        despublicada.setEstadoPublicacion("BORRADOR");

        when(actividadRepository.findAllById(any())).thenReturn(List.of(
                actividadPublicaConId(70L),
                actividadPublicaConId(71L),
                despublicada
        ));

        assertTrue(service.actividadesMasVistas(7, 3, 6).isEmpty());
    }

    private Actividad actividadPublicaConId(Long id) {
        Actividad actividad = new Actividad();
        try {
            java.lang.reflect.Field campo = Actividad.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(actividad, id);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        return actividad;
    }
}
