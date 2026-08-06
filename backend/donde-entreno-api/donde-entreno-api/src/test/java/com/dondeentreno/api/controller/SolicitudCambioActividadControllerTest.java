package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.CampoCambioDTO;
import com.dondeentreno.api.dto.SolicitudCambioActividadRequestDTO;
import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.security.CustomUserDetailsService;
import com.dondeentreno.api.security.RestAccessDeniedHandler;
import com.dondeentreno.api.security.RestAuthenticationEntryPoint;
import com.dondeentreno.api.security.SecurityConfig;
import com.dondeentreno.api.service.SolicitudCambioActividadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SolicitudCambioActividadController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "dondeentreno.auth.jwt.secret=clave-ficticia-de-test-con-longitud-suficiente-123456")
class SolicitudCambioActividadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudCambioActividadService solicitudCambioService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void crearSolicitudCambioSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(post("/api/publicador/actividades/70/solicitudes-cambio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nuevo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearSolicitudCambioConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(post("/api/publicador/actividades/70/solicitudes-cambio")
                        .with(jwtConRol("USUARIO", 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearSolicitudCambioConRolPublicadorDevuelveCreated() throws Exception {
        SolicitudCambioDetalleDTO detalle = new SolicitudCambioDetalleDTO();
        detalle.setId(80L);
        detalle.setActividadId(70L);
        detalle.setEstado("PENDIENTE");
        detalle.setCambios(List.of(new CampoCambioDTO("titulo", "Viejo", "Nuevo")));

        when(solicitudCambioService.crearSolicitud(
                eq(20L), eq(70L), any(SolicitudCambioActividadRequestDTO.class)
        )).thenReturn(detalle);

        mockMvc.perform(post("/api/publicador/actividades/70/solicitudes-cambio")
                        .with(jwtConRol("PUBLICADOR", 20L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nuevo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(80))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.cambios[0].campo").value("titulo"));
    }

    @Test
    void listarSolicitudesCambioSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/publicador/solicitudes-cambio"))
                .andExpect(status().isUnauthorized());
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
