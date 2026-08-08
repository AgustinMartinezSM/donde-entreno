package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenPublicadorDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenPublicadorServiceTest {

    private static final byte[] PNG_MINIMO = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private AlmacenArchivos almacenArchivos;

    private ImagenPublicadorService service;

    @BeforeEach
    void setUp() {
        service = new ImagenPublicadorService(
                imagenRepository,
                perfilPublicadorRepository,
                actividadRepository,
                almacenArchivos
        );
    }

    @Test
    void subirImagenGuardaPendienteEnElEspacioPrivado() {
        prepararPerfilYActividad(5L, 20L, 10L);

        when(almacenArchivos.guardarPendiente(any(), eq("actividades/10"), eq("png")))
                .thenReturn("actividades/10/uuid.png");
        when(imagenRepository.findByActividad_IdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(almacenArchivos.estaConfigurado()).thenReturn(true);
        when(almacenArchivos.firmarUrl(eq("actividades/10/uuid.png"), any(Duration.class)))
                .thenReturn("https://storage/firmada");

        ImagenPublicadorDTO dto = service.subirImagen(
                5L,
                10L,
                new MockMultipartFile("archivo", "foto.png", "image/png", PNG_MINIMO),
                "PRINCIPAL"
        );

        ArgumentCaptor<Imagen> captor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(captor.capture());
        Imagen guardada = captor.getValue();

        assertEquals("actividades/10/uuid.png", guardada.getUrl());
        assertEquals("PENDIENTE", guardada.getEstadoModeracion());
        assertFalse(guardada.getActiva());
        /* La preview del publicador usa la URL firmada del espacio privado. */
        assertEquals("https://storage/firmada", dto.getUrl());
    }

    @Test
    void subirImagenRechazaContenidoQueNoEsImagen() {
        prepararPerfilYActividad(5L, 20L, 10L);

        assertThrows(ImagenInvalidaException.class, () -> service.subirImagen(
                5L,
                10L,
                new MockMultipartFile(
                        "archivo",
                        "nota.txt",
                        "image/png",
                        "esto no es una imagen".getBytes()
                ),
                "PRINCIPAL"
        ));

        verify(almacenArchivos, never()).guardarPendiente(any(), anyString(), anyString());
    }

    @Test
    void eliminarMiaBorraElArchivoYDejaBajaLogica() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "actividades/10/uuid.png", "PENDIENTE");
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));

        service.eliminarMia(5L, 10L, 77L);

        verify(almacenArchivos).eliminar("actividades/10/uuid.png");
        assertEquals("RECHAZADA", imagen.getEstadoModeracion());
        assertFalse(imagen.getActiva());
    }

    @Test
    void eliminarMiaSoloPermiteImagenesPendientes() {
        prepararPerfilYActividad(5L, 20L, 10L);

        Imagen imagen = crearImagen(77L, "https://storage/publica.png", "APROBADA");
        when(imagenRepository.findByIdAndActividad_Id(77L, 10L))
                .thenReturn(Optional.of(imagen));

        assertThrows(
                ImagenInvalidaException.class,
                () -> service.eliminarMia(5L, 10L, 77L)
        );

        verify(almacenArchivos, never()).eliminar(anyString());
    }

    private void prepararPerfilYActividad(Long userId, Long perfilId, Long actividadId) {
        PerfilPublicador perfil = mock(PerfilPublicador.class);
        when(perfil.getId()).thenReturn(perfilId);
        when(perfilPublicadorRepository.findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(perfil));

        Actividad actividad = new Actividad();
        actividad.setId(actividadId);
        when(actividadRepository
                .findByIdAndPerfilPublicador_IdAndActivaTrueAndEstadoPublicacionAndDeletedAtIsNull(
                        actividadId,
                        perfilId,
                        "PUBLICADA"
                ))
                .thenReturn(Optional.of(actividad));
    }

    private Imagen crearImagen(Long id, String url, String estado) {
        Imagen imagen = new Imagen();
        imagen.setId(id);
        imagen.setUrl(url);
        imagen.setEstadoModeracion(estado);
        imagen.setActiva(false);
        imagen.setCreatedAt(OffsetDateTime.now());
        return imagen;
    }
}
