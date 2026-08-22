package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.CheckinRespuestaDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.EntrenamientoUsuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EntrenamientoUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckinServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long ACTIVIDAD_ID = 70L;

    @Mock
    private EntrenamientoUsuarioRepository entrenamientoUsuarioRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private CheckinService service;

    @BeforeEach
    void setUp() {
        service = new CheckinService(entrenamientoUsuarioRepository, actividadRepository);
    }

    @Test
    void registrarCreaLaFilaCuandoNoHayCheckinHoy() {
        configurarActividadPublica();
        when(entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        eq(USUARIO_ID), eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(entrenamientoUsuarioRepository
                .contarPersonasDesde(eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(5L);

        CheckinRespuestaDTO respuesta = service.registrar(USUARIO_ID, ACTIVIDAD_ID);

        assertTrue(respuesta.isYaRegistradoHoy());
        assertTrue(respuesta.isRegistradoAhora());
        assertEquals(5L, respuesta.getCantidadPersonasEntrenaron30Dias());

        ArgumentCaptor<EntrenamientoUsuario> captor =
                ArgumentCaptor.forClass(EntrenamientoUsuario.class);
        verify(entrenamientoUsuarioRepository).saveAndFlush(captor.capture());
        assertEquals(USUARIO_ID, captor.getValue().getUsuarioId());
        assertEquals(ACTIVIDAD_ID, captor.getValue().getActividadId());
    }

    @Test
    void registrarEsIdempotenteSiYaHayCheckinHoy() {
        configurarActividadPublica();
        when(entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        eq(USUARIO_ID), eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(true);
        when(entrenamientoUsuarioRepository
                .contarPersonasDesde(eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(3L);

        CheckinRespuestaDTO respuesta = service.registrar(USUARIO_ID, ACTIVIDAD_ID);

        assertTrue(respuesta.isYaRegistradoHoy());
        assertFalse(respuesta.isRegistradoAhora());
        verify(entrenamientoUsuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarSobreActividadNoPublicaDa404SinDelatarla() {
        Actividad pausada = actividadPublica();
        pausada.setEstadoPublicacion("PAUSADA");
        when(actividadRepository.findById(ACTIVIDAD_ID)).thenReturn(Optional.of(pausada));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.registrar(USUARIO_ID, ACTIVIDAD_ID)
        );
        verify(entrenamientoUsuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarSinUsuarioExigeAutenticacion() {
        assertThrows(
                CredencialesInvalidasException.class,
                () -> service.registrar(null, ACTIVIDAD_ID)
        );
    }

    @Test
    void estadoDeHoyNoCreaFilas() {
        configurarActividadPublica();
        when(entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        eq(USUARIO_ID), eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(true);
        when(entrenamientoUsuarioRepository
                .contarPersonasDesde(eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(8L);

        CheckinRespuestaDTO respuesta = service.estadoDeHoy(USUARIO_ID, ACTIVIDAD_ID);

        assertTrue(respuesta.isYaRegistradoHoy());
        assertFalse(respuesta.isRegistradoAhora());
        assertEquals(8L, respuesta.getCantidadPersonasEntrenaron30Dias());
        verify(entrenamientoUsuarioRepository, never()).saveAndFlush(any());
    }

    private void configurarActividadPublica() {
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));
    }

    private Actividad actividadPublica() {
        Actividad actividad = new Actividad();
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        return actividad;
    }
}
