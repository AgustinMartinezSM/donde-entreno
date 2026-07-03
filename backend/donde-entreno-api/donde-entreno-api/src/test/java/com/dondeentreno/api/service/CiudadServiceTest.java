package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.repository.CiudadRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CiudadServiceTest {

    @Mock
    private CiudadRepository ciudadRepository;

    private CiudadService ciudadService;

    @BeforeEach
    void setUp() {
        ciudadService = new CiudadService(ciudadRepository);
    }

    @Test
    void obtenerCiudadesActivasUsaOrdenEditorialYMapeaCamposTerritoriales() {
        when(ciudadRepository.findByActivaTrueOrderByOrdenAscNombreAsc())
                .thenReturn(List.of(ciudad(1L, "Mar del Plata", "mar-del-plata", 1, true)));

        List<CiudadDTO> ciudades = ciudadService.obtenerCiudadesActivas();

        assertEquals(1, ciudades.size());
        CiudadDTO ciudad = ciudades.get(0);
        assertEquals(1L, ciudad.getId());
        assertEquals("Mar del Plata", ciudad.getNombre());
        assertEquals("Buenos Aires", ciudad.getProvincia());
        assertEquals("Argentina", ciudad.getPais());
        assertEquals("mar-del-plata", ciudad.getSlug());
        assertEquals(1, ciudad.getOrden());
        assertEquals(true, ciudad.getActiva());

        verify(ciudadRepository).findByActivaTrueOrderByOrdenAscNombreAsc();
    }

    @Test
    void obtenerCiudadActivaPorSlugNormalizaSlugYDevuelveDto() {
        when(ciudadRepository.findBySlugAndActivaTrue("mar-del-plata"))
                .thenReturn(Optional.of(ciudad(1L, "Mar del Plata", "mar-del-plata", 1, true)));

        CiudadDTO ciudad = ciudadService.obtenerCiudadActivaPorSlug("  MAR-DEL-PLATA  ");

        assertEquals("mar-del-plata", ciudad.getSlug());
        assertEquals("Mar del Plata", ciudad.getNombre());
        assertEquals(1, ciudad.getOrden());
        assertEquals(true, ciudad.getActiva());

        verify(ciudadRepository).findBySlugAndActivaTrue("mar-del-plata");
    }

    @Test
    void obtenerCiudadActivaPorSlugInexistenteLanzaRecursoNoEncontrado() {
        when(ciudadRepository.findBySlugAndActivaTrue("no-existe"))
                .thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> ciudadService.obtenerCiudadActivaPorSlug("no-existe")
        );

        assertEquals("Ciudad no encontrada.", exception.getMessage());
    }

    private Ciudad ciudad(Long id, String nombre, String slug, Integer orden, Boolean activa) {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(id);
        ciudad.setNombre(nombre);
        ciudad.setProvincia("Buenos Aires");
        ciudad.setPais("Argentina");
        ciudad.setSlug(slug);
        ciudad.setOrden(orden);
        ciudad.setActiva(activa);
        return ciudad;
    }
}
