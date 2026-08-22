package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.MeGustaImagen;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaImagenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikesFotosServiceTest {

    private MeGustaImagenRepository meGustaRepository;
    private ImagenRepository imagenRepository;
    private LikesFotosService service;

    @BeforeEach
    void preparar() {
        meGustaRepository = mock(MeGustaImagenRepository.class);
        imagenRepository = mock(ImagenRepository.class);
        service = new LikesFotosService(meGustaRepository, imagenRepository);
    }

    @Test
    void darLikePersisteSobreUnaFotoAprobadaYActiva() {
        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(true, "APROBADA")));
        when(meGustaRepository.existsByUsuarioIdAndImagenId(7L, 30L)).thenReturn(false);

        service.dar(7L, 30L);

        ArgumentCaptor<MeGustaImagen> captor = ArgumentCaptor.forClass(MeGustaImagen.class);
        verify(meGustaRepository).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getUsuarioId());
        assertEquals(30L, captor.getValue().getImagenId());
    }

    @Test
    void darLikeEsIdempotente() {
        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(true, "APROBADA")));
        when(meGustaRepository.existsByUsuarioIdAndImagenId(7L, 30L)).thenReturn(true);

        service.dar(7L, 30L);

        verify(meGustaRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaFotoNoVisibleDa404SinDelatarla() {
        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(true, "PENDIENTE")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.dar(7L, 30L));

        when(imagenRepository.findById(31L)).thenReturn(Optional.of(imagen(false, "APROBADA")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.dar(7L, 31L));

        when(imagenRepository.findById(32L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.dar(7L, 32L));
    }

    @Test
    void darLikeSobreviveLaCarreraDelUnique() {
        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(true, "APROBADA")));
        when(meGustaRepository.existsByUsuarioIdAndImagenId(7L, 30L)).thenReturn(false);
        when(meGustaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        service.dar(7L, 30L);
    }

    @Test
    void quitarEsIdempotenteYNoConsultaLaFoto() {
        service.quitar(7L, 30L);

        verify(meGustaRepository).deleteByUsuarioIdAndImagenId(7L, 30L);
        verify(imagenRepository, never()).findById(any());
    }

    @Test
    void listarIdsDevuelveLosPropios() {
        when(meGustaRepository.imagenIdsDe(7L)).thenReturn(List.of(30L, 31L));

        assertEquals(List.of(30L, 31L), service.listarIds(7L));
    }

    private Imagen imagen(boolean activa, String estadoModeracion) {
        Imagen imagen = new Imagen();
        imagen.setActiva(activa);
        imagen.setEstadoModeracion(estadoModeracion);
        return imagen;
    }
}
