package com.dondeentreno.api.controller;

import com.dondeentreno.api.asistente.LimitadorConsultas;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.service.AsistenteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AsistenteController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AsistenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsistenteService asistenteService;

    @MockitoBean
    private LimitadorConsultas limitadorConsultas;

    @Test
    void respondeLaConsultaCuandoEstaDentroDelLimite() throws Exception {
        when(limitadorConsultas.registrarConsulta(anyString())).thenReturn(true);
        when(asistenteService.responder("busco yoga")).thenReturn(new AsistenteRespuestaDTO(
                "Encontré 2 actividades de Yoga.",
                List.of(new AsistenteEnlaceDTO("/explorar?deporteSlug=yoga&page=0", "Ver Yoga")),
                List.of("¿Cómo contacto a un club?"),
                "local"
        ));

        mockMvc.perform(post("/api/asistente/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"busco yoga\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Encontré 2 actividades de Yoga."))
                .andExpect(jsonPath("$.enlaces[0].href").value("/explorar?deporteSlug=yoga&page=0"))
                .andExpect(jsonPath("$.fuente").value("local"));
    }

    @Test
    void devuelve429SinLlamarAlServiceCuandoSeSuperaElLimite() throws Exception {
        when(limitadorConsultas.registrarConsulta(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/asistente/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"busco yoga\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));

        verifyNoInteractions(asistenteService);
    }

    @Test
    void devuelve400ConTextoVacio() throws Exception {
        when(limitadorConsultas.registrarConsulta(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/asistente/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.texto").value("Escribí una consulta."));

        verifyNoInteractions(asistenteService);
    }
}
