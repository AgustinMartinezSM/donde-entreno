package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.service.ActividadService;
import com.dondeentreno.api.service.HorarioActividadService;
import com.dondeentreno.api.service.ImagenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ActividadController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class ActividadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActividadService actividadService;

    @MockitoBean
    private HorarioActividadService horarioActividadService;

    @MockitoBean
    private ImagenService imagenService;

    @BeforeEach
    void setUp() {
        when(actividadService.buscarActividadesConFiltrosPaginado(
                nullable(Long.class),
                nullable(String.class),
                nullable(Long.class),
                nullable(String.class),
                nullable(Long.class),
                nullable(Long.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                anyInt(),
                anyInt(),
                anyString()
        )).thenReturn(new PaginaResponseDTO<ActividadDTO>(
                List.of(),
                0,
                10,
                0,
                0,
                true
        ));
    }

    @Test
    void listarActividadesConCiudadSlugPasaFiltroAlService() throws Exception {
        mockMvc.perform(get("/api/actividades")
                        .param("ciudadSlug", "mar-del-plata"))
                .andExpect(status().isOk());

        verify(actividadService).buscarActividadesConFiltrosPaginado(
                isNull(),
                isNull(),
                isNull(),
                eq("mar-del-plata"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(10),
                eq("recientes")
        );
    }

    @Test
    void listarActividadesConCiudadIdSiguePasandoFiltroAlService() throws Exception {
        mockMvc.perform(get("/api/actividades")
                        .param("ciudadId", "1"))
                .andExpect(status().isOk());

        verify(actividadService).buscarActividadesConFiltrosPaginado(
                isNull(),
                isNull(),
                eq(1L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(10),
                eq("recientes")
        );
    }

    @Test
    void listarActividadesConCiudadIdYCiudadSlugPasaAmbosFiltrosAlService() throws Exception {
        mockMvc.perform(get("/api/actividades")
                        .param("ciudadId", "1")
                        .param("ciudadSlug", "mar-del-plata"))
                .andExpect(status().isOk());

        verify(actividadService).buscarActividadesConFiltrosPaginado(
                isNull(),
                isNull(),
                eq(1L),
                eq("mar-del-plata"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(10),
                eq("recientes")
        );
    }
}
