package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.InteresActividad;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.InteresActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteresActividadServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long ACTIVIDAD_ID = 70L;

    @Mock
    private InteresActividadRepository interesActividadRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private InteresActividadService service;

    @BeforeEach
    void setUp() {
        service = new InteresActividadService(interesActividadRepository, actividadRepository);
    }

    @Test
    void marcarQuieroProbarCreaLaFila() {
        configurarActividadPublica();
        when(interesActividadRepository.findByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(Optional.empty());

        String estado = service.marcar(USUARIO_ID, ACTIVIDAD_ID, "QUIERO_PROBAR");

        assertEquals("QUIERO_PROBAR", estado);
        ArgumentCaptor<InteresActividad> captor =
                ArgumentCaptor.forClass(InteresActividad.class);
        verify(interesActividadRepository).saveAndFlush(captor.capture());
        assertEquals("QUIERO_PROBAR", captor.getValue().getEstado());
    }

    @Test
    void marcarYaProbeTransicionaLaFilaExistente() {
        configurarActividadPublica();
        InteresActividad existente = new InteresActividad();
        existente.setUsuarioId(USUARIO_ID);
        existente.setActividadId(ACTIVIDAD_ID);
        existente.setEstado("QUIERO_PROBAR");
        existente.setCreatedAt(OffsetDateTime.now());
        ponerId(existente);
        when(interesActividadRepository.findByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(Optional.of(existente));

        service.marcar(USUARIO_ID, ACTIVIDAD_ID, "YA_PROBE");

        assertEquals("YA_PROBE", existente.getEstado());
        verify(interesActividadRepository).save(existente);
        verify(interesActividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void estadoInvalidoDa400YActividadNoPublica404() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.marcar(USUARIO_ID, ACTIVIDAD_ID, "ME_ENCANTA")
        );

        Actividad pausada = actividadPublica();
        pausada.setEstadoPublicacion("PAUSADA");
        when(actividadRepository.findById(ACTIVIDAD_ID)).thenReturn(Optional.of(pausada));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.marcar(USUARIO_ID, ACTIVIDAD_ID, "QUIERO_PROBAR")
        );
    }

    private void configurarActividadPublica() {
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));
    }

    private Actividad actividadPublica() {
        Actividad actividad = new Actividad();
        try {
            java.lang.reflect.Field campo = Actividad.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(actividad, ACTIVIDAD_ID);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        actividad.setActiva(true);
        actividad.setEstadoPublicacion("PUBLICADA");
        return actividad;
    }

    private void ponerId(InteresActividad interes) {
        try {
            java.lang.reflect.Field campo = InteresActividad.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(interes, 1L);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
