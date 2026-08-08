package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.repository.ImagenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de asignarImagenPrincipal: el enriquecimiento de los DTOs
 * públicos con la imagen PRINCIPAL aprobada, en un query batch.
 */
@ExtendWith(MockitoExtension.class)
class ImagenServicePrincipalTest {

    @Mock
    private ImagenRepository imagenRepository;

    @Test
    void asignaLaImagenPrincipalPorActividadYDejaNullLasQueNoTienen() {
        ImagenService service = new ImagenService(imagenRepository);

        ActividadDTO conImagen = new ActividadDTO();
        conImagen.setId(1L);
        ActividadDTO sinImagen = new ActividadDTO();
        sinImagen.setId(2L);

        when(imagenRepository
                .findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
                        eq("APROBADA"),
                        eq("PRINCIPAL"),
                        anyCollection()
                ))
                .thenReturn(List.of(crearImagen(1L, "/uploads/actividades/boxeo.jpg", 0)));

        service.asignarImagenPrincipal(List.of(conImagen, sinImagen));

        assertEquals("/uploads/actividades/boxeo.jpg", conImagen.getImagenPrincipalUrl());
        assertNull(sinImagen.getImagenPrincipalUrl());
    }

    @Test
    void conVariasPrincipalesGanaLaDeMenorOrden() {
        ImagenService service = new ImagenService(imagenRepository);

        ActividadDTO actividad = new ActividadDTO();
        actividad.setId(1L);

        /* El repo devuelve ordenado por orden ASC: la primera gana. */
        when(imagenRepository
                .findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
                        eq("APROBADA"),
                        eq("PRINCIPAL"),
                        anyCollection()
                ))
                .thenReturn(List.of(
                        crearImagen(1L, "/uploads/primera.jpg", 0),
                        crearImagen(1L, "/uploads/segunda.jpg", 1)
                ));

        service.asignarImagenPrincipal(List.of(actividad));

        assertEquals("/uploads/primera.jpg", actividad.getImagenPrincipalUrl());
    }

    @Test
    void conListaVaciaNoConsultaElRepositorio() {
        ImagenService service = new ImagenService(imagenRepository);

        service.asignarImagenPrincipal(List.of());

        verifyNoInteractions(imagenRepository);
    }

    private Imagen crearImagen(Long actividadId, String url, int orden) {
        Actividad actividad = new Actividad();
        actividad.setId(actividadId);

        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl(url);
        imagen.setOrden(orden);

        return imagen;
    }
}
