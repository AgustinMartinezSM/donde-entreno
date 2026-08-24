package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Notificacion;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(notificacionRepository);
    }

    @Test
    void emitirGuardaLaNotificacionConLeidaFalse() {
        service.emitir(10L, "NUEVO_SEGUIDOR", "Tenés un seguidor nuevo.", "/publicador");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getUsuarioId());
        assertEquals("NUEVO_SEGUIDOR", captor.getValue().getTipo());
        assertEquals(Boolean.FALSE, captor.getValue().getLeida());
    }

    /*
      La emisión NUNCA rompe al flujo que la origina: aprobar una
      actividad vale más que avisarla.
    */
    @Test
    void emitirNoLanzaAunqueGuardarFalle() {
        when(notificacionRepository.save(any()))
                .thenThrow(new RuntimeException("base caida"));

        assertDoesNotThrow(() ->
                service.emitir(10L, "TIPO", "Titulo", null));
    }

    @Test
    void emitirConDatosIncompletosNoGuardaNada() {
        service.emitir(null, "TIPO", "Titulo", null);
        service.emitir(10L, null, "Titulo", null);
        service.emitir(10L, "TIPO", null, null);

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void emitirATodosGuardaUnaPorUsuarioYNoLanzaSiFalla() {
        service.emitirATodos(List.of(1L, 2L, 3L), "ACTIVIDAD_NUEVA", "Titulo", "/x");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionRepository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());

        when(notificacionRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("base caida"));
        assertDoesNotThrow(() ->
                service.emitirATodos(List.of(1L), "ACTIVIDAD_NUEVA", "Titulo", "/x"));
    }

    @Test
    void marcarLeidaSoloTocaLasPropias() {
        Notificacion ajena = new Notificacion();
        ajena.setUsuarioId(99L);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.marcarLeida(10L, 5L)
        );
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void marcarLeidaPropiaLaActualiza() {
        Notificacion propia = new Notificacion();
        propia.setUsuarioId(10L);
        propia.setLeida(Boolean.FALSE);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(propia));

        service.marcarLeida(10L, 5L);

        assertTrue(propia.getLeida());
        verify(notificacionRepository).save(propia);
    }
}
