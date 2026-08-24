package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicadorServiceTest {

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Mock
    private ImagenService imagenService;

    @Mock
    private com.dondeentreno.api.repository.ActividadRepository actividadRepository;

    @Mock
    private com.dondeentreno.api.repository.ImagenRepository imagenRepository;

    @Mock
    private com.dondeentreno.api.repository.ValoracionRepository valoracionRepository;

    private PerfilPublicadorService perfilPublicadorService;

    @BeforeEach
    void setUp() {
        perfilPublicadorService = new PerfilPublicadorService(
                perfilPublicadorRepository,
                seguimientoPublicadorRepository,
                imagenService,
                actividadRepository,
                imagenRepository,
                valoracionRepository
        );
    }

    @Test
    void obtenerPerfilesActivosMapeaLosCamposPublicos() {
        when(perfilPublicadorRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(perfil(8L, "Club Atlético Sur", "CLUB")));
        when(seguimientoPublicadorRepository.contarSeguidoresPorPerfiles(List.of(8L)))
                .thenReturn(List.of(conteo(8L, 12L)));

        List<PerfilPublicadorDTO> perfiles = perfilPublicadorService.obtenerPerfilesActivos();

        assertEquals(1, perfiles.size());
        assertEquals(8L, perfiles.get(0).getId());
        assertEquals("Club Atlético Sur", perfiles.get(0).getNombre());
        assertEquals(12L, perfiles.get(0).getCantidadSeguidores());

        verify(perfilPublicadorRepository).findByActivoTrueOrderByNombreAsc();
    }

    /*
      El listado resuelve los seguidores con un solo query agrupado: si
      hiciera un conteo por perfil, N perfiles serían N+1 queries.
    */
    @Test
    void obtenerPerfilesActivosCuentaSeguidoresEnUnSoloQueryYCompletaConCero() {
        when(perfilPublicadorRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(
                        perfil(8L, "Club Atlético Sur", "CLUB"),
                        perfil(9L, "Gimnasio Norte", "GIMNASIO")
                ));
        when(seguimientoPublicadorRepository.contarSeguidoresPorPerfiles(List.of(8L, 9L)))
                .thenReturn(List.of(conteo(8L, 3L)));

        List<PerfilPublicadorDTO> perfiles = perfilPublicadorService.obtenerPerfilesActivos();

        assertEquals(3L, perfiles.get(0).getCantidadSeguidores());
        assertEquals(0L, perfiles.get(1).getCantidadSeguidores(),
                "Un perfil sin seguidores no vuelve en el GROUP BY y debe quedar en cero, no en null.");

        verify(seguimientoPublicadorRepository).contarSeguidoresPorPerfiles(List.of(8L, 9L));
        verify(seguimientoPublicadorRepository, never()).countByPerfilPublicador_Id(any());
    }

    @Test
    void obtenerPerfilActivoPorIdDevuelveElDetalleSinTraerElListado() {
        when(perfilPublicadorRepository.findByIdAndActivoTrue(8L))
                .thenReturn(Optional.of(perfil(8L, "Club Atlético Sur", "CLUB")));
        when(seguimientoPublicadorRepository.countByPerfilPublicador_Id(8L)).thenReturn(5L);

        PerfilPublicadorDTO perfil = perfilPublicadorService.obtenerPerfilActivoPorId(8L);

        assertEquals(8L, perfil.getId());
        assertEquals("Club Atlético Sur", perfil.getNombre());
        assertEquals("CLUB", perfil.getTipoPublicador());
        assertEquals(5L, perfil.getCantidadSeguidores());

        verify(perfilPublicadorRepository).findByIdAndActivoTrue(8L);
    }

    /*
      El perfil inactivo no existe para el público: el repositorio ya
      filtra por activo, así que el service solo traduce el vacío a 404.
    */
    @Test
    void obtenerPerfilActivoPorIdInexistenteOInactivoLanzaRecursoNoEncontrado() {
        when(perfilPublicadorRepository.findByIdAndActivoTrue(999L))
                .thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> perfilPublicadorService.obtenerPerfilActivoPorId(999L)
        );

        assertEquals(
                "El perfil publicador solicitado no existe o no está disponible.",
                exception.getMessage()
        );
    }

    @Test
    void obtenerPerfilActivoPorIdNuloNoConsultaElRepositorio() {
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> perfilPublicadorService.obtenerPerfilActivoPorId(null)
        );

        verifyNoInteractions(perfilPublicadorRepository);
    }

    @Test
    void obtenerPorIdOSlugConNumeroResuelvePorIdComoSiempre() {
        when(perfilPublicadorRepository.findByIdAndActivoTrue(8L))
                .thenReturn(Optional.of(perfil(8L, "Club Atlético Sur", "CLUB")));
        when(seguimientoPublicadorRepository.countByPerfilPublicador_Id(8L)).thenReturn(0L);

        PerfilPublicadorDTO perfil =
                perfilPublicadorService.obtenerPerfilActivoPorIdOSlug("8");

        assertEquals(8L, perfil.getId());
        verify(perfilPublicadorRepository).findByIdAndActivoTrue(8L);
        verify(perfilPublicadorRepository, never()).findBySlugAndActivoTrue(any());
    }

    @Test
    void obtenerPorIdOSlugConTextoResuelvePorSlug() {
        PerfilPublicador entidad = perfil(8L, "Club Atlético Sur", "CLUB");
        entidad.setSlug("club-atletico-sur");
        when(perfilPublicadorRepository.findBySlugAndActivoTrue("club-atletico-sur"))
                .thenReturn(Optional.of(entidad));
        when(seguimientoPublicadorRepository.countByPerfilPublicador_Id(8L)).thenReturn(0L);

        PerfilPublicadorDTO perfil =
                perfilPublicadorService.obtenerPerfilActivoPorIdOSlug("club-atletico-sur");

        assertEquals(8L, perfil.getId());
        assertEquals("club-atletico-sur", perfil.getSlug());
        verify(perfilPublicadorRepository, never()).findByIdAndActivoTrue(any());
    }

    @Test
    void obtenerPorIdOSlugInexistenteLanzaRecursoNoEncontrado() {
        when(perfilPublicadorRepository.findBySlugAndActivoTrue("no-existe"))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> perfilPublicadorService.obtenerPerfilActivoPorIdOSlug("no-existe")
        );
    }

    private SeguimientoPublicadorRepository.ConteoSeguidores conteo(Long perfilId, long cantidad) {
        return new SeguimientoPublicadorRepository.ConteoSeguidores() {
            @Override
            public Long getPerfilPublicadorId() {
                return perfilId;
            }

            @Override
            public long getCantidad() {
                return cantidad;
            }
        };
    }

    private PerfilPublicador perfil(Long id, String nombre, String tipoPublicador) {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(id);
        perfil.setNombre(nombre);
        perfil.setTipoPublicador(tipoPublicador);
        perfil.setActivo(true);
        return perfil;
    }
}
