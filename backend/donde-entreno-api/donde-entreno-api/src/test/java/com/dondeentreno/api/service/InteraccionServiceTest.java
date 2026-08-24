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
}
