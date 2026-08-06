package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.SolicitudCambioActividadRequestDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudCambioActividad;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudCambioConflictoException;
import com.dondeentreno.api.exception.SolicitudCambioInvalidaException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudCambioActividadServiceTest {

    @Mock
    private SolicitudCambioActividadRepository solicitudCambioRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private SolicitudCambioActividadService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudCambioActividadService(
                solicitudCambioRepository,
                perfilPublicadorRepository,
                actividadRepository
        );
    }

    @Test
    void crearSolicitudNormalizaCamposYGuardaPendiente() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);
        when(solicitudCambioRepository.existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
                eq(70L), anyList()
        )).thenReturn(false);
        when(solicitudCambioRepository.save(any(SolicitudCambioActividad.class)))
                .thenAnswer((invocacion) -> invocacion.getArgument(0));

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("  Nuevo titulo  ");
        request.setNivel("principiante");
        request.setInstagramContacto("   ");

        SolicitudCambioDetalleDTO detalle = service.crearSolicitud(10L, 70L, request);

        ArgumentCaptor<SolicitudCambioActividad> captor =
                ArgumentCaptor.forClass(SolicitudCambioActividad.class);
        verify(solicitudCambioRepository).save(captor.capture());
        SolicitudCambioActividad guardada = captor.getValue();

        assertEquals("PENDIENTE", guardada.getEstado());
        assertEquals("Nuevo titulo", guardada.getTitulo());
        assertEquals("PRINCIPIANTE", guardada.getNivel());
        assertNull(guardada.getInstagramContacto());
        assertEquals(70L, detalle.getActividadId());
        assertEquals(2, detalle.getCambios().size());
        assertEquals("titulo", detalle.getCambios().get(0).getCampo());
        assertEquals("Titulo actual", detalle.getCambios().get(0).getValorActual());
        assertEquals("Nuevo titulo", detalle.getCambios().get(0).getValorPropuesto());
    }

    @Test
    void crearSolicitudConOtraAbiertaLanzaConflicto() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);
        when(solicitudCambioRepository.existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
                eq(70L), anyList()
        )).thenReturn(true);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("Nuevo titulo");

        assertThrows(
                SolicitudCambioConflictoException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
        verify(solicitudCambioRepository, never()).save(any(SolicitudCambioActividad.class));
    }

    @Test
    void crearSolicitudSinCambiosLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("   ");

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void crearSolicitudConNivelInvalidoLanzaInvalida() {
        PerfilPublicador perfil = perfil();
        Actividad actividad = actividad();
        configurarPerfil(perfil);
        configurarActividadPropia(actividad, perfil);

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setNivel("EXPERTO");

        assertThrows(
                SolicitudCambioInvalidaException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void crearSolicitudSobreActividadAjenaLanzaNoEncontrado() {
        PerfilPublicador perfil = perfil();
        configurarPerfil(perfil);
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        eq(70L), eq(30L), eq("PUBLICADA")
                ))
                .thenReturn(Optional.empty());

        SolicitudCambioActividadRequestDTO request = new SolicitudCambioActividadRequestDTO();
        request.setTitulo("Nuevo titulo");

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.crearSolicitud(10L, 70L, request)
        );
    }

    @Test
    void listarMiasConEstadoInvalidoLanzaFiltroInvalido() {
        PerfilPublicador perfil = perfil();
        configurarPerfil(perfil);

        assertThrows(
                FiltroInvalidoException.class,
                () -> service.listarMias(10L, "CUALQUIERA", 0, 20, null)
        );
    }

    @Test
    void obtenerMiaInexistenteLanzaNoEncontrado() {
        PerfilPublicador perfil = perfil();
        configurarPerfil(perfil);
        when(solicitudCambioRepository.findByIdAndPerfilPublicador_IdAndDeletedAtIsNull(99L, 30L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.obtenerMia(10L, 99L)
        );
    }

    private void configurarPerfil(PerfilPublicador perfil) {
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(perfil));
    }

    private void configurarActividadPropia(Actividad actividad, PerfilPublicador perfil) {
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        eq(70L), eq(perfil.getId()), eq("PUBLICADA")
                ))
                .thenReturn(Optional.of(actividad));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setActivo(true);
        usuario.setDeletedAt(null);
        return usuario;
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(30L);
        perfil.setUsuario(usuario());
        perfil.setNombre("Perfil Publicador");
        perfil.setActivo(true);
        return perfil;
    }

    private Actividad actividad() {
        Actividad actividad = new Actividad();
        actividad.setId(70L);
        actividad.setTitulo("Titulo actual");
        actividad.setSlug("titulo-actual");
        actividad.setNivel("TODOS");
        actividad.setPrecioReferencia(new BigDecimal("1000.00"));
        return actividad;
    }
}
