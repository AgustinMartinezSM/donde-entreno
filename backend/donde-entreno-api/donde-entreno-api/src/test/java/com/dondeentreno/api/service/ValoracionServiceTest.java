package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ResumenValoracionesDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.entity.Valoracion;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.EntrenamientoUsuarioRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.repository.ValoracionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValoracionServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long ACTIVIDAD_ID = 70L;

    @Mock
    private ValoracionRepository valoracionRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private FavoritoActividadRepository favoritoActividadRepository;

    @Mock
    private InteresActividadService interesActividadService;

    @Mock
    private EntrenamientoUsuarioRepository entrenamientoUsuarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacionService notificacionService;

    private ValoracionService service;

    @BeforeEach
    void setUp() {
        service = new ValoracionService(
                valoracionRepository,
                actividadRepository,
                favoritoActividadRepository,
                interesActividadService,
                entrenamientoUsuarioRepository,
                usuarioRepository,
                notificacionService
        );
    }

    @Test
    void sinSenalDeUsoNoSePuedeValorar() {
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));
        when(favoritoActividadRepository.existsByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(false);
        when(interesActividadService.tieneInteres(USUARIO_ID, ACTIVIDAD_ID)).thenReturn(false);
        when(entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        eq(USUARIO_ID), eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(false);

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.valorar(USUARIO_ID, ACTIVIDAD_ID, 5, null, null)
        );
    }

    @Test
    void conFavoritoValoraPeroNoQuedaVerificada() {
        configurarValoracionNueva();
        when(favoritoActividadRepository.existsByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(true);
        when(interesActividadService.yaProbo(USUARIO_ID, ACTIVIDAD_ID)).thenReturn(false);
        lenient().when(entrenamientoUsuarioRepository
                .existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
                        eq(USUARIO_ID), eq(ACTIVIDAD_ID), any(OffsetDateTime.class)))
                .thenReturn(false);

        service.valorar(USUARIO_ID, ACTIVIDAD_ID, 4, " Muy buena clase ", List.of("BUEN_AMBIENTE", "TAG_INVENTADO"));

        ArgumentCaptor<Valoracion> captor = ArgumentCaptor.forClass(Valoracion.class);
        verify(valoracionRepository).save(captor.capture());
        assertFalse(captor.getValue().getVerificada());
        assertEquals("Muy buena clase", captor.getValue().getComentario());
        /* Los tags fuera de catálogo se descartan en silencio. */
        assertEquals("BUEN_AMBIENTE", captor.getValue().getTags());
    }

    @Test
    void conYaProbeQuedaVerificadaYNotificaAlPublicador() {
        configurarValoracionNueva();
        when(interesActividadService.tieneInteres(USUARIO_ID, ACTIVIDAD_ID)).thenReturn(true);
        when(interesActividadService.yaProbo(USUARIO_ID, ACTIVIDAD_ID)).thenReturn(true);

        service.valorar(USUARIO_ID, ACTIVIDAD_ID, 5, null, null);

        ArgumentCaptor<Valoracion> captor = ArgumentCaptor.forClass(Valoracion.class);
        verify(valoracionRepository).save(captor.capture());
        assertTrue(captor.getValue().getVerificada());
        verify(notificacionService).emitir(
                eq(50L), eq("VALORACION_NUEVA"), any(), any()
        );
    }

    @Test
    void elPromedioNoVeajaConMenosDeTres() {
        when(valoracionRepository.countByActividadIdAndEstado(ACTIVIDAD_ID, "VISIBLE"))
                .thenReturn(2L);

        double[] resultado = service.promedioYCantidad(ACTIVIDAD_ID);

        assertEquals(-1.0, resultado[0]);
        assertEquals(2.0, resultado[1]);
    }

    @Test
    void elResumenCalculaPromedioYDistribucion() {
        Valoracion visible = new Valoracion();
        visible.setUsuarioId(USUARIO_ID);
        visible.setPuntaje(5);
        visible.setEstado("VISIBLE");
        visible.setCreatedAt(OffsetDateTime.now());

        when(valoracionRepository.findByActividadIdAndEstadoOrderByCreatedAtDesc(
                eq(ACTIVIDAD_ID), eq("VISIBLE"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(visible), PageRequest.of(0, 20), 4));
        when(valoracionRepository.distribucionVisibles(ACTIVIDAD_ID))
                .thenReturn(List.<Object[]>of(
                        new Object[]{5, 3L},
                        new Object[]{4, 1L}
                ));
        Usuario autor = new Usuario();
        autor.setNombre("Agustín");
        autor.setApellido("Martínez");
        ponerId(autor, USUARIO_ID);
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(autor));

        ResumenValoracionesDTO resumen = service.resumenDe(ACTIVIDAD_ID, USUARIO_ID, 0, 20);

        assertEquals(4.8, resumen.getPromedio());
        assertEquals(4L, resumen.getCantidad());
        assertEquals(3L, resumen.getDistribucion().get(5));
        assertEquals("Agustín M.", resumen.getContenido().get(0).getAutorNombre());
        assertTrue(resumen.getContenido().get(0).isEsPropia());
    }

    @Test
    void puntajeFueraDeRangoDa400() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> service.valorar(USUARIO_ID, ACTIVIDAD_ID, 6, null, null)
        );
        assertNull(null);
    }

    private void configurarValoracionNueva() {
        when(actividadRepository.findById(ACTIVIDAD_ID))
                .thenReturn(Optional.of(actividadPublica()));
        when(valoracionRepository.findByUsuarioIdAndActividadId(USUARIO_ID, ACTIVIDAD_ID))
                .thenReturn(Optional.empty());
        when(valoracionRepository.save(any(Valoracion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());
    }

    private Actividad actividadPublica() {
        Usuario duenio = new Usuario();
        ponerId(duenio, 50L);

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

    private void ponerId(Object entidad, Long id) {
        try {
            java.lang.reflect.Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
