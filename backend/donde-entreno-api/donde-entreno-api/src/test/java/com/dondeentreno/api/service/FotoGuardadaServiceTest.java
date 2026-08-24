package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.FotoGuardada;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.FotoGuardadaRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FotoGuardadaServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long IMAGEN_ID = 7L;

    @Mock
    private FotoGuardadaRepository fotoGuardadaRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private ImagenService imagenService;

    private FotoGuardadaService service;

    @BeforeEach
    void setUp() {
        service = new FotoGuardadaService(
                fotoGuardadaRepository,
                imagenRepository,
                imagenService
        );
    }

    @Test
    void guardarEsIdempotenteYSoloFotosVisibles() {
        Imagen visible = imagen(IMAGEN_ID, true, "APROBADA");
        when(imagenRepository.findById(IMAGEN_ID)).thenReturn(Optional.of(visible));
        when(fotoGuardadaRepository.existsByUsuarioIdAndImagenId(USUARIO_ID, IMAGEN_ID))
                .thenReturn(true);

        service.guardar(USUARIO_ID, IMAGEN_ID);
        verify(fotoGuardadaRepository, never()).saveAndFlush(any());

        Imagen oculta = imagen(8L, false, "APROBADA");
        when(imagenRepository.findById(8L)).thenReturn(Optional.of(oculta));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.guardar(USUARIO_ID, 8L)
        );
    }

    @Test
    void listarVisiblesOmiteLasDespublicadasSinPerderElGuardado() {
        FotoGuardada guardadaVisible = fotoGuardada(IMAGEN_ID);
        FotoGuardada guardadaOculta = fotoGuardada(8L);
        when(fotoGuardadaRepository.findByUsuarioIdOrderByCreatedAtDesc(USUARIO_ID))
                .thenReturn(List.of(guardadaVisible, guardadaOculta));
        when(imagenRepository.findAllById(List.of(IMAGEN_ID, 8L)))
                .thenReturn(List.of(
                        imagen(IMAGEN_ID, true, "APROBADA"),
                        imagen(8L, false, "APROBADA")
                ));
        when(imagenService.conLikes(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var visibles = service.listarVisibles(USUARIO_ID);

        assertEquals(1, visibles.size());
        assertEquals(IMAGEN_ID, visibles.get(0).getId());
    }

    private FotoGuardada fotoGuardada(Long imagenId) {
        FotoGuardada guardada = new FotoGuardada();
        guardada.setUsuarioId(USUARIO_ID);
        guardada.setImagenId(imagenId);
        guardada.setCreatedAt(OffsetDateTime.now());
        return guardada;
    }

    private Imagen imagen(Long id, boolean activa, String estado) {
        Imagen imagen = new Imagen();
        try {
            java.lang.reflect.Field campo = Imagen.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(imagen, id);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        imagen.setActiva(activa);
        imagen.setEstadoModeracion(estado);
        imagen.setUrl("https://storage/publica/" + id + ".jpg");
        return imagen;
    }
}
