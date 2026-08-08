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

    private static final String URL_PUBLICA =
            "https://proyecto.supabase.co/storage/v1/object/public/imagenes-publicas/boxeo.jpg";

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
                .thenReturn(List.of(crearImagen(1L, URL_PUBLICA, 0)));

        service.asignarImagenPrincipal(List.of(conImagen, sinImagen));

        assertEquals(URL_PUBLICA, conImagen.getImagenPrincipalUrl());
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
                        crearImagen(1L, "https://cdn.test/primera.jpg", 0),
                        crearImagen(1L, "https://cdn.test/segunda.jpg", 1)
                ));

        service.asignarImagenPrincipal(List.of(actividad));

        assertEquals("https://cdn.test/primera.jpg", actividad.getImagenPrincipalUrl());
    }

    /*
      Las filas sembradas antes de Supabase Storage guardan rutas de disco
      local que la API nunca sirvió. Exponerlas hacía que el frontend
      pidiera una imagen inexistente en vez de caer a su ilustración.
    */
    @Test
    void ignoraLasUrlsRelativasLegadoDeDiscoLocal() {
        ImagenService service = new ImagenService(imagenRepository);

        ActividadDTO actividad = new ActividadDTO();
        actividad.setId(1L);

        when(imagenRepository
                .findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
                        eq("APROBADA"),
                        eq("PRINCIPAL"),
                        anyCollection()
                ))
                .thenReturn(List.of(crearImagen(1L, "/uploads/actividades/boxeo.jpg", 0)));

        service.asignarImagenPrincipal(List.of(actividad));

        assertNull(actividad.getImagenPrincipalUrl());
    }

    @Test
    void conUnaRelativaYUnaAbsolutaGanaLaAbsolutaAunqueTengaMayorOrden() {
        ImagenService service = new ImagenService(imagenRepository);

        ActividadDTO actividad = new ActividadDTO();
        actividad.setId(1L);

        when(imagenRepository
                .findByActivaTrueAndEstadoModeracionAndTipoImagenAndActividad_IdInOrderByOrdenAsc(
                        eq("APROBADA"),
                        eq("PRINCIPAL"),
                        anyCollection()
                ))
                .thenReturn(List.of(
                        crearImagen(1L, "/uploads/legado.jpg", 0),
                        crearImagen(1L, URL_PUBLICA, 1)
                ));

        service.asignarImagenPrincipal(List.of(actividad));

        assertEquals(URL_PUBLICA, actividad.getImagenPrincipalUrl());
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
