package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ImagenAdminDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.exception.ImagenInvalidaException;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.storage.AlmacenArchivos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenAdminServiceTest {

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private AlmacenArchivos almacenArchivos;

    private ImagenAdminService service;

    @BeforeEach
    void setUp() {
        service = new ImagenAdminService(imagenRepository, almacenArchivos);
    }

    @Test
    void aprobarPublicaElArchivoYDesactivaLaPrincipalAnterior() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);

        Imagen pendiente = crearImagen(77L, "actividades/10/nueva.png", "PENDIENTE");
        pendiente.setTipoImagen("PRINCIPAL");
        pendiente.setActividad(actividad);

        Imagen principalAnterior = crearImagen(50L, "https://storage/vieja.png", "APROBADA");
        principalAnterior.setTipoImagen("PRINCIPAL");
        principalAnterior.setActiva(true);

        when(imagenRepository.findById(77L)).thenReturn(Optional.of(pendiente));
        when(almacenArchivos.publicar("actividades/10/nueva.png"))
                .thenReturn("https://storage/publica/nueva.png");
        when(imagenRepository.findByActividad_IdAndTipoImagenAndActivaTrue(10L, "PRINCIPAL"))
                .thenReturn(List.of(principalAnterior));
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImagenAdminDTO dto = service.aprobar(77L);

        assertEquals("APROBADA", pendiente.getEstadoModeracion());
        assertTrue(pendiente.getActiva());
        assertEquals("https://storage/publica/nueva.png", pendiente.getUrl());
        assertEquals("https://storage/publica/nueva.png", dto.getUrl());
        /* El reemplazo de PRINCIPAL desactiva la anterior, no la borra. */
        assertFalse(principalAnterior.getActiva());
    }

    @Test
    void aprobarNoTocaLaBaseSiElStorageFalla() {
        Imagen pendiente = crearImagen(77L, "actividades/10/nueva.png", "PENDIENTE");
        when(imagenRepository.findById(77L)).thenReturn(Optional.of(pendiente));
        when(almacenArchivos.publicar(anyString()))
                .thenThrow(new IllegalStateException("storage caido"));

        assertThrows(IllegalStateException.class, () -> service.aprobar(77L));

        verify(imagenRepository, never()).save(any());
        assertEquals("PENDIENTE", pendiente.getEstadoModeracion());
    }

    @Test
    void rechazarEliminaElArchivoYGuardaElMotivo() {
        Imagen pendiente = crearImagen(77L, "actividades/10/nueva.png", "PENDIENTE");
        when(imagenRepository.findById(77L)).thenReturn(Optional.of(pendiente));
        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.rechazar(77L, "  No corresponde a la actividad.  ");

        verify(almacenArchivos).eliminar("actividades/10/nueva.png");
        assertEquals("RECHAZADA", pendiente.getEstadoModeracion());
        assertEquals("No corresponde a la actividad.", pendiente.getMotivoRechazo());
        assertFalse(pendiente.getActiva());
    }

    @Test
    void rechazarExigeMotivo() {
        Imagen pendiente = crearImagen(77L, "actividades/10/nueva.png", "PENDIENTE");
        when(imagenRepository.findById(77L)).thenReturn(Optional.of(pendiente));

        assertThrows(ImagenInvalidaException.class, () -> service.rechazar(77L, "   "));

        verify(almacenArchivos, never()).eliminar(anyString());
        verify(imagenRepository, never()).save(any());
    }

    @Test
    void unaImagenYaRevisadaNoSePuedeVolverAModerar() {
        Imagen aprobada = crearImagen(77L, "https://storage/publica.png", "APROBADA");
        when(imagenRepository.findById(77L)).thenReturn(Optional.of(aprobada));

        assertThrows(ImagenInvalidaException.class, () -> service.aprobar(77L));
        assertThrows(
                ImagenInvalidaException.class,
                () -> service.rechazar(77L, "motivo")
        );
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
