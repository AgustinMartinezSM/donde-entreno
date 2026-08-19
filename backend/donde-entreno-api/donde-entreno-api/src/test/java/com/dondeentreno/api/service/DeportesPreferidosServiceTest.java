package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.DeportePreferido;
import com.dondeentreno.api.repository.DeportePreferidoRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeportesPreferidosServiceTest {

    private DeportePreferidoRepository preferidoRepository;
    private DeporteRepository deporteRepository;
    private DeportesPreferidosService service;

    @BeforeEach
    void preparar() {
        preferidoRepository = mock(DeportePreferidoRepository.class);
        deporteRepository = mock(DeporteRepository.class);
        service = new DeportesPreferidosService(preferidoRepository, deporteRepository);
    }

    @Test
    void reemplazarFiltraContraElCatalogoColapsaDuplicadosYConservaElOrden() {
        when(deporteRepository.findByActivoTrue()).thenReturn(List.of(
                deporte(1L, "yoga"),
                deporte(2L, "karate"),
                deporte(3L, "natacion")
        ));

        List<String> guardados = service.reemplazar(7L, Arrays.asList(
                " karate ",
                "inventado",
                "yoga",
                "karate",
                null,
                "  "
        ));

        /* Lo devuelto es lo efectivamente guardado, en el orden elegido. */
        assertEquals(List.of("karate", "yoga"), guardados);
        verify(preferidoRepository).borrarDe(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeportePreferido>> captor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(preferidoRepository).saveAll(captor.capture());
        List<DeportePreferido> filas = captor.getValue();

        assertEquals(2, filas.size());
        assertEquals(2L, filas.get(0).getDeporteId());
        assertEquals(1L, filas.get(1).getDeporteId());
        assertEquals(7L, filas.get(0).getUsuarioId());
        /* El orden de eleccion sobrevive al ORDER BY (createdAt, id). */
        assertTrue(filas.get(0).getCreatedAt().isBefore(filas.get(1).getCreatedAt()));
    }

    @Test
    void reemplazarConListaVaciaDejaAlUsuarioSinDeportes() {
        when(deporteRepository.findByActivoTrue()).thenReturn(List.of(deporte(1L, "yoga")));

        List<String> guardados = service.reemplazar(7L, List.of());

        assertTrue(guardados.isEmpty());
        verify(preferidoRepository).borrarDe(7L);
        verify(preferidoRepository).saveAll(List.of());
    }

    @Test
    void listarDelegaEnElQueryDeSlugs() {
        when(preferidoRepository.slugsDe(7L)).thenReturn(List.of("yoga", "karate"));

        assertEquals(List.of("yoga", "karate"), service.listar(7L));
    }

    private Deporte deporte(Long id, String slug) {
        Deporte deporte = new Deporte();
        deporte.setId(id);
        deporte.setSlug(slug);
        deporte.setNombre("Deporte " + slug);
        deporte.setActivo(Boolean.TRUE);
        return deporte;
    }
}
