package com.dondeentreno.api.service;

import com.dondeentreno.api.exception.FiltroInvalidoException;
import com.dondeentreno.api.repository.ActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private HorarioActividadService horarioActividadService;

    @Mock
    private ImagenService imagenService;

    @Mock
    private SocialProofService socialProofService;

    private ActividadService actividadService;

    @BeforeEach
    void setUp() {
        actividadService = new ActividadService(
                actividadRepository,
                horarioActividadService,
                imagenService,
                socialProofService
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoNormalizaCiudadSlug() {
        configurarPaginaVacia();

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                null,
                "  MAR-DEL-PLATA  ",
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                eq("mar-del-plata"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoMantieneCiudadIdYCiudadSlug() {
        configurarPaginaVacia();

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                1L,
                "mar-del-plata",
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                eq(1L),
                eq("mar-del-plata"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoMantieneCiudadIdSinCiudadSlug() {
        configurarPaginaVacia();

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                eq(1L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoNormalizaNivelYModalidadValidos() {
        configurarPaginaVacia();

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                null,
                null,
                null,
                null,
                " principiante ",
                "online",
                null,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("PRINCIPIANTE"),
                eq("ONLINE"),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoSinNivelNiModalidadNoAplicaEsosFiltros() {
        configurarPaginaVacia();

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                null,
                null,
                null,
                null,
                "   ",
                null,
                null,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoConNivelInvalidoLanzaFiltroInvalido() {
        FiltroInvalidoException exception = assertThrows(
                FiltroInvalidoException.class,
                () -> actividadService.buscarActividadesConFiltrosPaginado(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "EXPERTO",
                        null,
                        null,
                        0,
                        10,
                        "recientes"
                )
        );

        assertEquals(
                "El parametro 'nivel' tiene un valor invalido: 'EXPERTO'. "
                        + "Valores permitidos: PRINCIPIANTE, INTERMEDIO, AVANZADO, TODOS.",
                exception.getMessage()
        );
        verifyNoInteractions(actividadRepository);
    }

    @Test
    void buscarActividadesConFiltrosPaginadoConModalidadInvalidaLanzaFiltroInvalido() {
        FiltroInvalidoException exception = assertThrows(
                FiltroInvalidoException.class,
                () -> actividadService.buscarActividadesConFiltrosPaginado(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "HIBRIDA",
                        null,
                        0,
                        10,
                        "recientes"
                )
        );

        assertEquals(
                "El parametro 'modalidad' tiene un valor invalido: 'HIBRIDA'. "
                        + "Valores permitidos: PRESENCIAL, ONLINE, MIXTA.",
                exception.getMessage()
        );
        verifyNoInteractions(actividadRepository);
    }

    @Test
    void buscarActividadesConFiltrosPaginadoConOrdenInvalidoLanzaFiltroInvalido() {
        FiltroInvalidoException exception = assertThrows(
                FiltroInvalidoException.class,
                () -> actividadService.buscarActividadesConFiltrosPaginado(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "ranking"
                )
        );

        assertEquals(
                "El parametro 'orden' tiene un valor invalido: 'ranking'. "
                        + "Valores permitidos: recientes, precio_asc, precio_desc, titulo_asc.",
                exception.getMessage()
        );
        verifyNoInteractions(actividadRepository);
    }

    @Test
    void buscarActividadesConFiltrosPaginadoConOrdenValidoAplicaElSort() {
        configurarPaginaVacia();

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                "precio_asc"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(""),
                pageableCaptor.capture()
        );

        Sort.Order ordenPrecio = pageableCaptor.getValue().getSort().getOrderFor("precioReferencia");
        assertNotNull(ordenPrecio);
        assertEquals(Sort.Direction.ASC, ordenPrecio.getDirection());
    }

    @Test
    void buscarActividadesConFiltrosNormalizaNivelYModalidadValidos() {
        actividadService.buscarActividadesConFiltros(
                null,
                null,
                null,
                null,
                null,
                null,
                "avanzado",
                "mixta",
                null
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltros(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("AVANZADO"),
                eq("MIXTA"),
                eq("")
        );
    }

    @Test
    void buscarActividadesConFiltrosConNivelInvalidoLanzaFiltroInvalido() {
        assertThrows(
                FiltroInvalidoException.class,
                () -> actividadService.buscarActividadesConFiltros(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "EXPERTO",
                        null,
                        null
                )
        );

        verifyNoInteractions(actividadRepository);
    }

    @Test
    void buscarActividadesConFiltrosPaginadoRecortaTextoDeBusquedaMuyLargo() {
        configurarPaginaVacia();

        String textoLargo = "a".repeat(200);

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                textoLargo,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("a".repeat(120)),
                any(Pageable.class)
        );
    }

    @Test
    void buscarActividadesConFiltrosPaginadoConservaTextoEnElLimite() {
        configurarPaginaVacia();

        String textoEnLimite = "b".repeat(120);

        actividadService.buscarActividadesConFiltrosPaginado(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                textoEnLimite,
                0,
                10,
                "recientes"
        );

        verify(actividadRepository).buscarActividadesPublicadasConFiltrosPaginado(
                eq("PUBLICADA"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(textoEnLimite),
                any(Pageable.class)
        );
    }

    private void configurarPaginaVacia() {
        when(actividadRepository.buscarActividadesPublicadasConFiltrosPaginado(
                anyString(),
                nullable(Long.class),
                nullable(String.class),
                nullable(Long.class),
                nullable(String.class),
                nullable(Long.class),
                nullable(Long.class),
                nullable(String.class),
                nullable(String.class),
                anyString(),
                any(Pageable.class)
        )).thenReturn(Page.empty());
    }
}
