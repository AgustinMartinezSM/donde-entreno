package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicadorServiceTest {

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    private PerfilPublicadorService perfilPublicadorService;

    @BeforeEach
    void setUp() {
        perfilPublicadorService = new PerfilPublicadorService(perfilPublicadorRepository);
    }

    @Test
    void obtenerPerfilesActivosMapeaLosCamposPublicos() {
        when(perfilPublicadorRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(perfil(8L, "Club Atlético Sur", "CLUB")));

        List<PerfilPublicadorDTO> perfiles = perfilPublicadorService.obtenerPerfilesActivos();

        assertEquals(1, perfiles.size());
        assertEquals(8L, perfiles.get(0).getId());
        assertEquals("Club Atlético Sur", perfiles.get(0).getNombre());

        verify(perfilPublicadorRepository).findByActivoTrueOrderByNombreAsc();
    }

    @Test
    void obtenerPerfilActivoPorIdDevuelveElDetalleSinTraerElListado() {
        when(perfilPublicadorRepository.findByIdAndActivoTrue(8L))
                .thenReturn(Optional.of(perfil(8L, "Club Atlético Sur", "CLUB")));

        PerfilPublicadorDTO perfil = perfilPublicadorService.obtenerPerfilActivoPorId(8L);

        assertEquals(8L, perfil.getId());
        assertEquals("Club Atlético Sur", perfil.getNombre());
        assertEquals("CLUB", perfil.getTipoPublicador());

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

    private PerfilPublicador perfil(Long id, String nombre, String tipoPublicador) {
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setId(id);
        perfil.setNombre(nombre);
        perfil.setTipoPublicador(tipoPublicador);
        perfil.setActivo(true);
        return perfil;
    }
}
