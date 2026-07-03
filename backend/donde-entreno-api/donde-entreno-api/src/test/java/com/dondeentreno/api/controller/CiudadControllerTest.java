package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.CiudadDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.service.CiudadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CiudadController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CiudadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CiudadService ciudadService;

    @Test
    void listarCiudadesActivasDevuelveCamposTerritoriales() throws Exception {
        when(ciudadService.obtenerCiudadesActivas())
                .thenReturn(List.of(ciudadDto()));

        mockMvc.perform(get("/api/ciudades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Mar del Plata"))
                .andExpect(jsonPath("$[0].provincia").value("Buenos Aires"))
                .andExpect(jsonPath("$[0].pais").value("Argentina"))
                .andExpect(jsonPath("$[0].slug").value("mar-del-plata"))
                .andExpect(jsonPath("$[0].orden").value(1))
                .andExpect(jsonPath("$[0].activa").value(true));
    }

    @Test
    void obtenerCiudadActivaPorSlugDevuelveCiudad() throws Exception {
        when(ciudadService.obtenerCiudadActivaPorSlug("mar-del-plata"))
                .thenReturn(ciudadDto());

        mockMvc.perform(get("/api/ciudades/mar-del-plata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("mar-del-plata"))
                .andExpect(jsonPath("$.nombre").value("Mar del Plata"))
                .andExpect(jsonPath("$.orden").value(1))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void obtenerCiudadActivaPorSlugInexistenteDevuelveNotFound() throws Exception {
        when(ciudadService.obtenerCiudadActivaPorSlug("no-existe"))
                .thenThrow(new RecursoNoEncontradoException("Ciudad no encontrada."));

        mockMvc.perform(get("/api/ciudades/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Ciudad no encontrada."))
                .andExpect(jsonPath("$.path").value("/api/ciudades/no-existe"));
    }

    private CiudadDTO ciudadDto() {
        return new CiudadDTO(
                1L,
                "Mar del Plata",
                "Buenos Aires",
                "Argentina",
                "mar-del-plata",
                1,
                true
        );
    }
}
