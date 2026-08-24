package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.PreguntaActividad;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.PreguntaActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreguntaActividadServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long DUENIO_ID = 50L;
    private static final Long ACTIVIDAD_ID = 70L;

    @Mock
    private PreguntaActividadRepository preguntaActividadRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private NotificacionService notificacionService;

    private PreguntaActividadService service;

    @BeforeEach
    void setUp() {
        service = new PreguntaActividadService(
                preguntaActividadRepository,
                actividadRepository,
                notificacionService
        );
    }

    @Test
    void preguntarNotificaAlPublicadorYRespetaElTope() {
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));
        when(preguntaActividadRepository
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(eq(USUARIO_ID), any()))
                .thenReturn(0L);
        when(preguntaActividadRepository.save(any(PreguntaActividad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        service.preguntar(USUARIO_ID, ACTIVIDAD_ID, "¿Hay clase de prueba?");

        verify(notificacionService).emitir(eq(DUENIO_ID), eq("PREGUNTA_NUEVA"), any(), any());
    }

    @Test
    void alQuintaPreguntaDelDiaCorta() {
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));
        when(preguntaActividadRepository
                .countByUsuarioIdAndCreatedAtGreaterThanEqual(eq(USUARIO_ID), any()))
                .thenReturn(5L);

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.preguntar(USUARIO_ID, ACTIVIDAD_ID, "Otra pregunta")
        );
    }

    @Test
    void soloElDuenioResponde() {
        PreguntaActividad pregunta = preguntaVisible();
        when(preguntaActividadRepository.findById(1L)).thenReturn(Optional.of(pregunta));
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));

        /* Otro usuario que no es el dueño: 404, sin delatarla. */
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.responder(999L, 1L, "Hola")
        );

        when(preguntaActividadRepository.save(any(PreguntaActividad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        service.responder(DUENIO_ID, 1L, "Sí, los sábados.");

        assertEquals("Sí, los sábados.", pregunta.getRespuesta());
        verify(notificacionService).emitir(eq(USUARIO_ID), eq("RESPUESTA_PREGUNTA"), any(), any());
    }

    @Test
    void unaPreguntaRespondidaNoSePuedeBorrar() {
        PreguntaActividad respondida = preguntaVisible();
        respondida.setRespuesta("Ya te contesto");
        when(preguntaActividadRepository.findById(1L)).thenReturn(Optional.of(respondida));

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.eliminarPropia(USUARIO_ID, 1L)
        );
    }

    @Test
    void borrarLaPropiaSinRespuestaLaMarcaEliminada() {
        PreguntaActividad pregunta = preguntaVisible();
        when(preguntaActividadRepository.findById(1L)).thenReturn(Optional.of(pregunta));
        when(preguntaActividadRepository.save(pregunta)).thenReturn(pregunta);

        service.eliminarPropia(USUARIO_ID, 1L);

        assertEquals("ELIMINADA_POR_USUARIO", pregunta.getEstado());
    }

    private PreguntaActividad preguntaVisible() {
        PreguntaActividad pregunta = new PreguntaActividad();
        pregunta.setActividadId(ACTIVIDAD_ID);
        pregunta.setUsuarioId(USUARIO_ID);
        pregunta.setPregunta("¿Hay clase de prueba?");
        pregunta.setEstado("VISIBLE");
        pregunta.setCreatedAt(OffsetDateTime.now());
        return pregunta;
    }

    private Actividad actividadPublica() {
        Usuario duenio = new Usuario();
        try {
            java.lang.reflect.Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(duenio, DUENIO_ID);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);

        Actividad actividad = new Actividad();
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        actividad.setTitulo("Karate");
        actividad.setSlug("karate");
        actividad.setPerfilPublicador(perfil);
        return actividad;
    }
}
