package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.AuthUsuarioDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.EmailYaRegistradoException;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.exception.RegistroInvalidoException;
import com.dondeentreno.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void registrarUsuarioValidoDevuelveCreatedConTokenYRolUsuario() throws Exception {
        when(authService.registrarUsuario(any()))
                .thenReturn(new LoginResponseDTO(
                        "Bearer",
                        "jwt-usuario",
                        3600L,
                        new AuthUsuarioDTO(10L, "usuario@ejemplo.com", "Usuario", "Prueba", "USUARIO")
                ));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistroUsuarioValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-usuario"))
                .andExpect(jsonPath("$.usuario.rol").value("USUARIO"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

        verify(authService).registrarUsuario(any());
    }

    @Test
    void registrarPublicadorValidoDevuelveCreatedConTokenYRolPublicador() throws Exception {
        when(authService.registrarPublicador(any()))
                .thenReturn(new LoginResponseDTO(
                        "Bearer",
                        "jwt-publicador",
                        3600L,
                        new AuthUsuarioDTO(20L, "publicador@ejemplo.com", "Publicador", "Prueba", "PUBLICADOR")
                ));

        mockMvc.perform(post("/api/auth/registro/publicador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistroPublicadorValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-publicador"))
                .andExpect(jsonPath("$.usuario.rol").value("PUBLICADOR"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

        verify(authService).registrarPublicador(any());
    }

    @Test
    void registroUsuarioConEmailInvalidoDevuelveBadRequestYNoLlamaService() throws Exception {
        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Usuario",
                                  "apellido": "Prueba",
                                  "email": "no-es-email",
                                  "password": "Password1",
                                  "confirmarPassword": "Password1",
                                  "telefono": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists());

        verify(authService, never()).registrarUsuario(any());
    }

    @Test
    void registroUsuarioConPasswordDebilDesdeServiceDevuelveBadRequest() throws Exception {
        when(authService.registrarUsuario(any()))
                .thenThrow(new RegistroInvalidoException("La password no cumple los requisitos minimos."));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistroUsuarioValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("La password no cumple los requisitos minimos."));
    }

    @Test
    void registroConEmailDuplicadoDesdeServiceDevuelveConflict() throws Exception {
        when(authService.registrarUsuario(any()))
                .thenThrow(new EmailYaRegistradoException("El email ya esta registrado."));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistroUsuarioValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El email ya esta registrado."));
    }

    @Test
    void obtenerMeSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."));

        verify(authService, never()).obtenerUsuarioActual(any());
    }

    @Test
    void obtenerMeConJwtDevuelveUsuarioActual() throws Exception {
        when(authService.obtenerUsuarioActual(10L))
                .thenReturn(new UsuarioActualDTO(
                        10L,
                        "Usuario",
                        "Prueba",
                        "usuario@ejemplo.com",
                        "USUARIO",
                        "+54 223 555 1234",
                        true,
                        true
                ));

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwtConUserId(10L)));

        try {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.email").value("usuario@ejemplo.com"))
                    .andExpect(jsonPath("$.rol").value("USUARIO"))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(authService).obtenerUsuarioActual(10L);
    }

    private String jsonValido() {
        return """
                {
                  "email": "admin@dondeentreno.com",
                  "password": "password-ficticio"
                }
                """;
    }

    private String jsonRegistroUsuarioValido() {
        return """
                {
                  "nombre": "Usuario",
                  "apellido": "Prueba",
                  "email": "usuario@ejemplo.com",
                  "password": "Password1",
                  "confirmarPassword": "Password1",
                  "telefono": "+54 223 555 1234"
                }
                """;
    }

    private String jsonRegistroPublicadorValido() {
        return """
                {
                  "nombre": "Publicador",
                  "apellido": "Prueba",
                  "email": "publicador@ejemplo.com",
                  "password": "Password1",
                  "confirmarPassword": "Password1",
                  "whatsapp": "+54 223 555 9999",
                  "tipoPublicador": "PROFESOR_INDEPENDIENTE",
                  "nombrePublico": "Perfil Publicador",
                  "ciudadPrincipalId": 1,
                  "descripcion": "Perfil de prueba",
                  "instagram": "@perfil",
                  "emailContacto": "contacto@ejemplo.com",
                  "telefonoContacto": "+54 223 555 8888"
                }
                """;
    }

    private Jwt jwtConUserId(Long userId) {
        Instant ahora = Instant.now();
        return Jwt.withTokenValue("jwt-ficticio")
                .header("alg", "none")
                .subject("usuario@ejemplo.com")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .claim("userId", userId)
                .build();
    }
}
