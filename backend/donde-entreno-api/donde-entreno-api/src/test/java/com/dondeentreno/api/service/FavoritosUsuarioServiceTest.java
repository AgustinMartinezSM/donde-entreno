package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.FavoritoActividad;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoritosUsuarioServiceTest {

    private FavoritoActividadRepository favoritoRepository;
    private ActividadRepository actividadRepository;
    private ImagenService imagenService;
    private FavoritosUsuarioService service;

    @BeforeEach
    void preparar() {
        favoritoRepository = mock(FavoritoActividadRepository.class);
        actividadRepository = mock(ActividadRepository.class);
        imagenService = mock(ImagenService.class);
        service = new FavoritosUsuarioService(
                favoritoRepository,
                actividadRepository,
                imagenService
        );
    }

    @Test
    void listarDevuelveLasCardsEnElOrdenDelFavoritoYLesAsignaImagen() {
        when(favoritoRepository.findByUsuarioIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(favorito(7L, 20L), favorito(7L, 10L)));
        /* El repo de actividades devuelve en CUALQUIER orden. */
        when(actividadRepository.findByIdInAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                anyCollection(), eq("PUBLICADA")
        )).thenReturn(List.of(actividad(10L, "yoga-suave"), actividad(20L, "karate-kids")));

        List<ActividadDTO> resultado = service.listar(7L);

        assertEquals(2, resultado.size());
        /* Manda el orden del favorito: 20 se guardo mas recientemente. */
        assertEquals("karate-kids", resultado.get(0).getSlug());
        assertEquals("yoga-suave", resultado.get(1).getSlug());
        verify(imagenService).asignarImagenPrincipal(resultado);
    }

    /*
      Una actividad despublicada desaparece de la lista sin romperla: el
      query filtrado no la devuelve y el armado la saltea.
    */
    @Test
    void listarSalteaLosFavoritosCuyaActividadYaNoEsPublica() {
        when(favoritoRepository.findByUsuarioIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(favorito(7L, 20L), favorito(7L, 10L)));
        when(actividadRepository.findByIdInAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                anyCollection(), eq("PUBLICADA")
        )).thenReturn(List.of(actividad(10L, "yoga-suave")));

        List<ActividadDTO> resultado = service.listar(7L);

        assertEquals(1, resultado.size());
        assertEquals("yoga-suave", resultado.get(0).getSlug());
    }

    @Test
    void listarSinFavoritosNoConsultaActividades() {
        when(favoritoRepository.findByUsuarioIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of());

        List<ActividadDTO> resultado = service.listar(7L);

        assertTrue(resultado.isEmpty());
        verify(actividadRepository, never())
                .findByIdInAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(anyCollection(), any());
    }

    @Test
    void guardarResuelveElSlugYPersisteElFavorito() {
        when(actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion("karate-kids", "PUBLICADA"))
                .thenReturn(Optional.of(actividad(20L, "karate-kids")));
        when(favoritoRepository.existsByUsuarioIdAndActividadId(7L, 20L)).thenReturn(false);

        service.guardar(7L, " karate-kids ");

        ArgumentCaptor<FavoritoActividad> captor = ArgumentCaptor.forClass(FavoritoActividad.class);
        verify(favoritoRepository).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getUsuarioId());
        assertEquals(20L, captor.getValue().getActividadId());
    }

    @Test
    void guardarEsIdempotenteSiYaEstabaGuardado() {
        when(actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion("karate-kids", "PUBLICADA"))
                .thenReturn(Optional.of(actividad(20L, "karate-kids")));
        when(favoritoRepository.existsByUsuarioIdAndActividadId(7L, 20L)).thenReturn(true);

        service.guardar(7L, "karate-kids");

        verify(favoritoRepository, never()).saveAndFlush(any());
    }

    @Test
    void guardarUnSlugInexistenteDevuelve404() {
        when(actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion("no-existe", "PUBLICADA"))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.guardar(7L, "no-existe"));
    }

    /*
      Dos requests guardando lo mismo a la vez: el segundo choca con el
      UNIQUE y el resultado es el mismo que si hubiera llegado tarde.
    */
    @Test
    void guardarSobreviveLaCarreraDelUnique() {
        when(actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion("karate-kids", "PUBLICADA"))
                .thenReturn(Optional.of(actividad(20L, "karate-kids")));
        when(favoritoRepository.existsByUsuarioIdAndActividadId(7L, 20L)).thenReturn(false);
        when(favoritoRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        service.guardar(7L, "karate-kids");
    }

    @Test
    void quitarBorraPorSlugYEsIdempotente() {
        when(actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion("karate-kids", "PUBLICADA"))
                .thenReturn(Optional.of(actividad(20L, "karate-kids")));

        service.quitar(7L, "karate-kids");

        verify(favoritoRepository).deleteByUsuarioIdAndActividadId(7L, 20L);
    }

    @Test
    void quitarUnSlugInexistenteNoExplotaNiBorra() {
        when(actividadRepository.findBySlugAndActivaTrueAndEstadoPublicacion("no-existe", "PUBLICADA"))
                .thenReturn(Optional.empty());

        service.quitar(7L, "no-existe");

        verify(favoritoRepository, never()).deleteByUsuarioIdAndActividadId(any(), any());
    }

    private FavoritoActividad favorito(Long usuarioId, Long actividadId) {
        FavoritoActividad favorito = new FavoritoActividad();
        favorito.setUsuarioId(usuarioId);
        favorito.setActividadId(actividadId);
        return favorito;
    }

    private Actividad actividad(Long id, String slug) {
        Actividad actividad = new Actividad();
        actividad.setId(id);
        actividad.setSlug(slug);
        actividad.setTitulo("Actividad " + slug);
        return actividad;
    }
}
