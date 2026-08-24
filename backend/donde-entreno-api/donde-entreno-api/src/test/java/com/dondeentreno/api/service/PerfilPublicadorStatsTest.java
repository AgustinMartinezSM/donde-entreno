package com.dondeentreno.api.service;

import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.ValoracionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Stats de cabecera del perfil (Fase 5, script 31): actividades,
 * fotos y valoraciones agregadas del publicador.
 */
@ExtendWith(MockitoExtension.class)
class PerfilPublicadorStatsTest {

    private static final Long PERFIL_ID = 8L;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Mock
    private ImagenService imagenService;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private ValoracionRepository valoracionRepository;

    private PerfilPublicadorService service;

    @BeforeEach
    void setUp() {
        service = new PerfilPublicadorService(
                perfilPublicadorRepository,
                seguimientoPublicadorRepository,
                imagenService,
                actividadRepository,
                imagenRepository,
                valoracionRepository
        );

        when(perfilPublicadorRepository.findByIdAndActivoTrue(PERFIL_ID))
                .thenReturn(Optional.of(perfil()));
        when(seguimientoPublicadorRepository.countByPerfilPublicador_Id(PERFIL_ID))
                .thenReturn(4L);
        when(imagenService.obtenerLogosAprobadosPorPerfil(anyList()))
                .thenReturn(java.util.Map.of());
    }

    @Test
    void conTresValoracionesOMasElPromedioSePublica() {
        when(actividadRepository.contarPublicadasPorPerfil(anyList(), anyString()))
                .thenReturn(List.<Object[]>of(new Object[]{PERFIL_ID, 6L}));
        when(imagenRepository.contarFotosVisiblesPorPublicador(anyList(), anyString()))
                .thenReturn(List.<Object[]>of(new Object[]{PERFIL_ID, 9L}));
        /* 5,5,4,4 sobre dos actividades distintas → 4.5 con N=4. */
        when(valoracionRepository.distribucionVisiblesPorPublicador(anyList()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{PERFIL_ID, 5, 2L},
                        new Object[]{PERFIL_ID, 4, 2L}
                ));

        var dto = service.obtenerPerfilActivoPorId(PERFIL_ID);

        assertEquals(6L, dto.getCantidadActividades());
        assertEquals(9L, dto.getCantidadFotos());
        assertEquals(4L, dto.getCantidadValoraciones());
        assertEquals(4.5, dto.getValoracionPromedio());
        assertEquals(4L, dto.getCantidadSeguidores());
    }

    /**
     * La regla que evita que el perfil contradiga a la actividad: con
     * menos de 3 el promedio NO se publica, pero la cantidad sí.
     */
    @Test
    void conMenosDeTresValoracionesElPromedioViajaNull() {
        when(actividadRepository.contarPublicadasPorPerfil(anyList(), anyString()))
                .thenReturn(List.of());
        when(imagenRepository.contarFotosVisiblesPorPublicador(anyList(), anyString()))
                .thenReturn(List.of());
        when(valoracionRepository.distribucionVisiblesPorPublicador(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{PERFIL_ID, 5, 2L}));

        var dto = service.obtenerPerfilActivoPorId(PERFIL_ID);

        assertNull(dto.getValoracionPromedio());
        assertEquals(2L, dto.getCantidadValoraciones());
        /* Sin filas en el GROUP BY, los contadores quedan en cero, no null. */
        assertEquals(0L, dto.getCantidadActividades());
        assertEquals(0L, dto.getCantidadFotos());
    }

    @Test
    void unPerfilSinNadaNoRompeYDevuelveCeros() {
        when(actividadRepository.contarPublicadasPorPerfil(anyList(), anyString()))
                .thenReturn(List.of());
        when(imagenRepository.contarFotosVisiblesPorPublicador(anyList(), anyString()))
                .thenReturn(List.of());
        when(valoracionRepository.distribucionVisiblesPorPublicador(anyList()))
                .thenReturn(List.of());

        var dto = service.obtenerPerfilActivoPorId(PERFIL_ID);

        assertEquals(0L, dto.getCantidadValoraciones());
        assertNull(dto.getValoracionPromedio());
    }

    private PerfilPublicador perfil() {
        PerfilPublicador perfil = new PerfilPublicador();
        try {
            java.lang.reflect.Field campo = PerfilPublicador.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(perfil, PERFIL_ID);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
        perfil.setNombre("Club Atletico Sur");
        perfil.setTipoPublicador("CLUB");
        perfil.setActivo(true);
        return perfil;
    }
}
