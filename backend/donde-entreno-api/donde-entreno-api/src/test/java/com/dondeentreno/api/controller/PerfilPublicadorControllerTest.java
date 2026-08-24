package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PerfilPublicadorDTO;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.service.ImagenService;
import com.dondeentreno.api.service.PerfilPublicadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PerfilPublicadorController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class PerfilPublicadorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilPublicadorService perfilPublicadorService;

    @MockitoBean
    private ImagenService imagenService;

    /* Fase 5: el controller suma opiniones, preguntas y tracking. */
    @MockitoBean
    private com.dondeentreno.api.service.ActividadService actividadService;

    @MockitoBean
    private com.dondeentreno.api.service.ValoracionService valoracionService;

    @MockitoBean
    private com.dondeentreno.api.service.PreguntaActividadService preguntaActividadService;

    @MockitoBean
    private com.dondeentreno.api.service.InteraccionService interaccionService;

    @MockitoBean
    private com.dondeentreno.api.security.LimitadorInteracciones limitadorInteracciones;

    @Test
    void obtenerPerfilPorIdDevuelveElDetallePublico() throws Exception {
        when(perfilPublicadorService.obtenerPerfilActivoPorIdOSlug("8"))
                .thenReturn(perfilDTO());

        mockMvc.perform(get("/api/perfiles-publicadores/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.nombre").value("Club Atlético Sur"))
                .andExpect(jsonPath("$.tipoPublicador").value("CLUB"))
                .andExpect(jsonPath("$.verificado").value(true));

        verify(perfilPublicadorService).obtenerPerfilActivoPorIdOSlug("8");
    }

    /* El path con slug llega al mismo service (script 27). */
    @Test
    void obtenerPerfilPorSlugLlegaAlService() throws Exception {
        when(perfilPublicadorService.obtenerPerfilActivoPorIdOSlug("club-atletico-sur"))
                .thenReturn(perfilDTO());

        mockMvc.perform(get("/api/perfiles-publicadores/club-atletico-sur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
    }

    /*
      El perfil inexistente o inactivo tiene que dar 404 y no 500: la
      página /publicadores/{id} del frontend lo traduce a su propio 404.
    */
    @Test
    void obtenerPerfilInexistenteDevuelve404() throws Exception {
        when(perfilPublicadorService.obtenerPerfilActivoPorIdOSlug("999"))
                .thenThrow(new RecursoNoEncontradoException(
                        "El perfil publicador solicitado no existe o no está disponible."
                ));

        mockMvc.perform(get("/api/perfiles-publicadores/999"))
                .andExpect(status().isNotFound());
    }

    private PerfilPublicadorDTO perfilDTO() {
        PerfilPublicadorDTO dto = new PerfilPublicadorDTO();
        dto.setId(8L);
        dto.setNombre("Club Atlético Sur");
        dto.setTipoPublicador("CLUB");
        dto.setVerificado(true);
        return dto;
    }
}
