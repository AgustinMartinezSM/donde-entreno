package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.EstadoSeguimientoDTO;
import com.dondeentreno.api.dto.SeguimientoPublicadorDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguimientoPublicadorServiceTest {

    @Mock
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private SeguimientoPublicadorService service;

    @BeforeEach
    void setUp() {
        service = new SeguimientoPublicadorService(
                seguimientoPublicadorRepository,
                perfilPublicadorRepository,
                usuarioRepository,
                actividadRepository
        );
    }

    @Test
    void seguirGuardaCuandoTodaviaNoLoSigue() {
        Long userId = 5L;
        Long perfilId = 20L;
        when(perfilPublicadorRepository.findByIdAndActivoTrue(perfilId))
                .thenReturn(Optional.of(mock(PerfilPublicador.class)));
        when(seguimientoPublicadorRepository.existsByUsuario_IdAndPerfilPublicador_Id(userId, perfilId))
                .thenReturn(false);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(mock(Usuario.class)));

        EstadoSeguimientoDTO estado = service.seguir(userId, perfilId);

        assertTrue(estado.isSiguiendo());
        verify(seguimientoPublicadorRepository).save(any(SeguimientoPublicador.class));
    }

    @Test
    void seguirEsIdempotenteSiYaLoSigue() {
        Long userId = 5L;
        Long perfilId = 20L;
        when(perfilPublicadorRepository.findByIdAndActivoTrue(perfilId))
                .thenReturn(Optional.of(mock(PerfilPublicador.class)));
        when(seguimientoPublicadorRepository.existsByUsuario_IdAndPerfilPublicador_Id(userId, perfilId))
                .thenReturn(true);

        EstadoSeguimientoDTO estado = service.seguir(userId, perfilId);

        assertTrue(estado.isSiguiendo());
        verify(seguimientoPublicadorRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void seguirAPublicadorInexistenteLanza404() {
        Long userId = 5L;
        Long perfilId = 999L;
        when(perfilPublicadorRepository.findByIdAndActivoTrue(perfilId))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.seguir(userId, perfilId)
        );

        verify(seguimientoPublicadorRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void dejarDeSeguirBorraElSeguimiento() {
        service.dejarDeSeguir(5L, 20L);
        verify(seguimientoPublicadorRepository)
                .deleteByUsuario_IdAndPerfilPublicador_Id(5L, 20L);
    }

    @Test
    void estadoReflejaSiExisteElSeguimiento() {
        when(seguimientoPublicadorRepository.existsByUsuario_IdAndPerfilPublicador_Id(5L, 20L))
                .thenReturn(true);

        assertTrue(service.estado(5L, 20L).isSiguiendo());
    }

    @Test
    void listarSeguidosMapeaLosDatosDelPerfil() {
        Ciudad ciudad = mock(Ciudad.class);
        when(ciudad.getNombre()).thenReturn("Mar del Plata");
        PerfilPublicador perfil = mock(PerfilPublicador.class);
        when(perfil.getId()).thenReturn(20L);
        when(perfil.getNombre()).thenReturn("Club Test");
        when(perfil.getTipoPublicador()).thenReturn("CLUB");
        when(perfil.getCiudadPrincipal()).thenReturn(ciudad);
        SeguimientoPublicador seguimiento = mock(SeguimientoPublicador.class);
        when(seguimiento.getPerfilPublicador()).thenReturn(perfil);
        when(seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(seguimiento));

        List<SeguimientoPublicadorDTO> seguidos = service.listarSeguidos(5L);

        assertEquals(1, seguidos.size());
        assertEquals(20L, seguidos.get(0).getPerfilPublicadorId());
        assertEquals("Club Test", seguidos.get(0).getPerfilPublicadorNombre());
        assertEquals("CLUB", seguidos.get(0).getTipoPublicador());
        assertEquals("Mar del Plata", seguidos.get(0).getCiudadPrincipalNombre());
    }

    @Test
    void feedSinSeguidosDevuelveVacioSinConsultarActividades() {
        when(seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of());

        List<ActividadDTO> feed = service.obtenerFeedActividades(5L);

        assertTrue(feed.isEmpty());
        verifyNoInteractions(actividadRepository);
    }

    @Test
    void feedConsultaLasActividadesPublicadasDeLosPerfilesSeguidos() {
        PerfilPublicador perfil = mock(PerfilPublicador.class);
        when(perfil.getId()).thenReturn(20L);
        SeguimientoPublicador seguimiento = mock(SeguimientoPublicador.class);
        when(seguimiento.getPerfilPublicador()).thenReturn(perfil);
        when(seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(seguimiento));
        when(actividadRepository
                .findByActivaTrueAndEstadoPublicacionAndDeletedAtIsNullAndPerfilPublicador_IdInOrderByCreatedAtDesc(
                        "PUBLICADA", List.of(20L), PageRequest.of(0, 20)))
                .thenReturn(List.of());

        List<ActividadDTO> feed = service.obtenerFeedActividades(5L);

        assertTrue(feed.isEmpty());
        verify(actividadRepository)
                .findByActivaTrueAndEstadoPublicacionAndDeletedAtIsNullAndPerfilPublicador_IdInOrderByCreatedAtDesc(
                        "PUBLICADA", List.of(20L), PageRequest.of(0, 20));
    }

    @Test
    void feedConUserIdNuloLanzaCredencialesInvalidas() {
        assertThrows(
                CredencialesInvalidasException.class,
                () -> service.obtenerFeedActividades(null)
        );
        verifyNoInteractions(actividadRepository);
    }

    @Test
    void conUserIdNuloLanzaCredencialesInvalidas() {
        assertThrows(
                CredencialesInvalidasException.class,
                () -> service.estado(null, 20L)
        );
        verifyNoInteractions(seguimientoPublicadorRepository);
    }
}
