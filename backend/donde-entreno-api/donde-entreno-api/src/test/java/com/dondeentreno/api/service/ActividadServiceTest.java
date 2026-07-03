package com.dondeentreno.api.service;

import com.dondeentreno.api.repository.ActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private HorarioActividadService horarioActividadService;

    @Mock
    private ImagenService imagenService;

    private ActividadService actividadService;

    @BeforeEach
    void setUp() {
        actividadService = new ActividadService(
                actividadRepository,
                horarioActividadService,
                imagenService
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
