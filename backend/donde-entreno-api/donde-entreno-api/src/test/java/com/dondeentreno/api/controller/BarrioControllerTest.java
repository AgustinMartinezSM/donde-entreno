package com.dondeentreno.api.controller;

import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.service.BarrioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BarrioController.class)
@Import(GlobalExceptionHandler.class)
class BarrioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarrioService barrioService;

    @Test
    void listarBarriosConCiudadIdInvalidoDevuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/barrios")
                        .param("ciudadId", "no-es-un-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Uno o mas parametros tienen un formato invalido."))
                .andExpect(jsonPath("$.errores.ciudadId").value("El parametro debe tener un valor valido."))
                .andExpect(jsonPath("$.path").value("/api/barrios"))
                .andExpect(content().string(not(containsString("no-es-un-numero"))));

        verifyNoInteractions(barrioService);
    }
}