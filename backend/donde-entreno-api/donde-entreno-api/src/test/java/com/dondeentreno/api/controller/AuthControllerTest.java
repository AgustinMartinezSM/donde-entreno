package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.AuthUsuarioDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginValidoDevuelveOkConTokenYUsuario() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResponseDTO(
                        "Bearer",
                        "jwt-ficticio",
                        3600L,
                        new AuthUsuarioDTO(1L, "admin@dondeentreno.com", "Admin", null, "SUPER_ADMIN")
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-ficticio"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.id").value(1))
                .andExpect(jsonPath("$.usuario.email").value("admin@dondeentreno.com"))
                .andExpect(jsonPath("$.usuario.rol").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

        verify(authService).login(any());
    }

    @Test
    void emailInvalidoDevuelveBadRequestYNoLlamaService() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "no-es-email",
                                  "password": "password-ficticio"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud contiene datos invalidos."))
                .andExpect(jsonPath("$.errores.email").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void passwordVacioDevuelveBadRequestYNoLlamaService() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@dondeentreno.com",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud contiene datos invalidos."))
                .andExpect(jsonPath("$.errores.password").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void credencialesInvalidasDevuelveUnauthorizedConMensajeGenerico() throws Exception {
        when(authService.login(any()))
                .thenThrow(new CredencialesInvalidasException("Email o password invalidos."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Email o password invalidos."))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    private String jsonValido() {
        return """
                {
                  "email": "admin@dondeentreno.com",
                  "password": "password-ficticio"
                }
                """;
    }
}
