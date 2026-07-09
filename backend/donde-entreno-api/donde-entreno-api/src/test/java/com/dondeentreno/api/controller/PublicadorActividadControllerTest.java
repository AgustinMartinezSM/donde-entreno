package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.PublicadorActividadDetalleDTO;
import com.dondeentreno.api.dto.PublicadorActividadHorarioDTO;
import com.dondeentreno.api.dto.PublicadorActividadImagenDTO;
import com.dondeentreno.api.dto.PublicadorActividadResumenDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.security.CustomUserDetailsService;
import com.dondeentreno.api.security.RestAccessDeniedHandler;
import com.dondeentreno.api.security.RestAuthenticationEntryPoint;
import com.dondeentreno.api.security.SecurityConfig;
import com.dondeentreno.api.service.PublicadorActividadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicadorActividadController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "dondeentreno.auth.jwt.secret=clave-ficticia-de-test-con-longitud-suficiente-123456")
class PublicadorActividadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicadorActividadService publicadorActividadService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listarActividadesSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/publicador/actividades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void listarActividadesConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/publicador/actividades")
                        .with(jwtConRol("USUARIO", 10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listarActividadesConRolAdminOSuperAdminDevuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/publicador/actividades")
                        .with(jwtConRol("ADMIN", 10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/publicador/actividades")
                        .with(jwtConRol("SUPER_ADMIN", 10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listarActividadesConRolPublicadorDevuelvePaginaYPasaParametrosAlService() throws Exception {
        when(publicadorActividadService.listarMisActividades(20L, 0, 20, "titulo_asc"))
                .thenReturn(new PaginaResponseDTO<>(
                        List.of(resumen()),
                        0,
                        20,
                        1,
                        1,
                        true
                ));

        mockMvc.perform(get("/api/publicador/actividades")
                        .param("page", "0")
                        .param("size", "20")
                        .param("orden", "titulo_asc")
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(100))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Boxeo recreativo"))
                .andExpect(jsonPath("$.contenido[0].slugPublico").value("boxeo-recreativo"))
                .andExpect(jsonPath("$.contenido[0].estadoPublicacion").value("PUBLICADA"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].passwordHash").doesNotExist());

        verify(publicadorActividadService).listarMisActividades(20L, 0, 20, "titulo_asc");
    }

    @Test
    void obtenerActividadConRolPublicadorDevuelveDetalle() throws Exception {
        when(publicadorActividadService.obtenerMiActividad(20L, 100L))
                .thenReturn(detalle());

        mockMvc.perform(get("/api/publicador/actividades/{id}", 100L)
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.titulo").value("Boxeo recreativo"))
                .andExpect(jsonPath("$.horarios[0].diaSemana").value("LUNES"))
                .andExpect(jsonPath("$.imagenes[0].tipoImagen").value("PRINCIPAL"))
                .andExpect(jsonPath("$.solicitudOrigenId").value(400))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(publicadorActividadService).obtenerMiActividad(20L, 100L);
    }

    @Test
    void obtenerActividadInexistenteDevuelveNotFound() throws Exception {
        when(publicadorActividadService.obtenerMiActividad(20L, 999L))
                .thenThrow(new RecursoNoEncontradoException("Actividad no encontrada."));

        mockMvc.perform(get("/api/publicador/actividades/{id}", 999L)
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Actividad no encontrada."));
    }

    private PublicadorActividadResumenDTO resumen() {
        return new PublicadorActividadResumenDTO(
                100L,
                "Boxeo recreativo",
                "boxeo-recreativo",
                "Boxeo",
                "boxeo",
                "Combate",
                "Mar del Plata",
                "mar-del-plata",
                "Centro",
                "PUBLICADA",
                true,
                "PRESENCIAL",
                "PRINCIPIANTE",
                18,
                60,
                new BigDecimal("18000.00"),
                true,
                "https://img.test/principal.jpg",
                OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                "boxeo-recreativo"
        );
    }

    private PublicadorActividadDetalleDTO detalle() {
        return new PublicadorActividadDetalleDTO(
                100L,
                "Boxeo recreativo",
                "boxeo-recreativo",
                "Boxeo",
                "boxeo",
                "Combate",
                "Mar del Plata",
                "mar-del-plata",
                "Centro",
                "PUBLICADA",
                true,
                "PRESENCIAL",
                "PRINCIPIANTE",
                18,
                60,
                new BigDecimal("18000.00"),
                true,
                "https://img.test/principal.jpg",
                OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                "boxeo-recreativo",
                "Clases para principiantes.",
                "RECREATIVO",
                false,
                false,
                "Escuela Norte",
                "Av. Independencia 1234",
                null,
                "+54 9 223 512-3456",
                "@escuelanorte",
                "contacto@ejemplo.com",
                30L,
                "Perfil Publicador",
                "PROFESOR_INDEPENDIENTE",
                400L,
                "DEP-20260704-ABC12345",
                List.of(new PublicadorActividadHorarioDTO(
                        200L,
                        "LUNES",
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 30),
                        null
                )),
                List.of(new PublicadorActividadImagenDTO(
                        300L,
                        "https://img.test/principal.jpg",
                        "PRINCIPAL",
                        "Portada",
                        null,
                        1
                ))
        );
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(
            String rol,
            Long userId
    ) {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
