package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.Reporte;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ValoracionService valoracionService;

    @Mock
    private PreguntaActividadService preguntaActividadService;

    @Mock
    private ComentarioImagenService comentarioImagenService;

    @Mock
    private NovedadService novedadService;

    @Mock
    private EventoDeportivoService eventoDeportivoService;

    @Mock
    private InboxService inboxService;

    @Mock
    private GrupoActividadService grupoActividadService;

    private ReporteService service;

    @BeforeEach
    void setUp() {
        service = new ReporteService(
                reporteRepository,
                imagenRepository,
                perfilPublicadorRepository,
                actividadRepository,
                valoracionService,
                preguntaActividadService,
                comentarioImagenService,
                novedadService,
                eventoDeportivoService,
                inboxService,
                grupoActividadService
        );
    }

    @Test
    void reportarUnaFotoVisibleCreaElReportePendiente() {
        when(imagenRepository.findById(7L)).thenReturn(Optional.of(imagenVisible()));
        when(reporteRepository.existsByUsuarioIdAndTipoObjetoAndObjetoId(10L, "IMAGEN", 7L))
                .thenReturn(false);

        service.reportar(10L, "IMAGEN", 7L, "SPAM", "  es publicidad  ");

        ArgumentCaptor<Reporte> captor = ArgumentCaptor.forClass(Reporte.class);
        verify(reporteRepository).saveAndFlush(captor.capture());
        assertEquals("PENDIENTE", captor.getValue().getEstado());
        assertEquals("es publicidad", captor.getValue().getDetalle());
    }

    @Test
    void reportarDosVecesLoMismoEsIdempotente() {
        when(imagenRepository.findById(7L)).thenReturn(Optional.of(imagenVisible()));
        when(reporteRepository.existsByUsuarioIdAndTipoObjetoAndObjetoId(10L, "IMAGEN", 7L))
                .thenReturn(true);

        service.reportar(10L, "IMAGEN", 7L, "SPAM", null);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaFotoNoVisibleDa404SinDelatarla() {
        Imagen pendiente = new Imagen();
        pendiente.setActiva(true);
        pendiente.setEstadoModeracion("PENDIENTE");
        when(imagenRepository.findById(7L)).thenReturn(Optional.of(pendiente));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.reportar(10L, "IMAGEN", 7L, "SPAM", null)
        );
    }

    @Test
    void tipoYMotivoFueraDeCatalogoDan400() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.reportar(10L, "CHAT", 7L, "SPAM", null)
        );
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.reportar(10L, "IMAGEN", 7L, "ME_ABURRE", null)
        );
    }

    @Test
    void cambiarEstadoValidaElCatalogo() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.cambiarEstado(1L, "CERRADO")
        );

        Reporte reporte = new Reporte();
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(reporte)).thenReturn(reporte);

        assertEquals("ACCIONADO", service.cambiarEstado(1L, "ACCIONADO").getEstado());
    }

    private Imagen imagenVisible() {
        Imagen imagen = new Imagen();
        imagen.setActiva(true);
        imagen.setEstadoModeracion("APROBADA");
        return imagen;
    }
}
