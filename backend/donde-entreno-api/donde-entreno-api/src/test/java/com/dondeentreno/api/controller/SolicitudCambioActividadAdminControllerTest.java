package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.SolicitudCambioDetalleDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.security.CustomUserDetailsService;
import com.dondeentreno.api.security.RestAccessDeniedHandler;
import com.dondeentreno.api.security.RestAuthenticationEntryPoint;
import com.dondeentreno.api.security.SecurityConfig;
import com.dondeentreno.api.service.SolicitudCambioActividadAdminService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SolicitudCambioActividadAdminController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "dondeentreno.auth.jwt.secret=clave-ficticia-de-test-con-longitud-suficiente-123456")
class SolicitudCambioActividadAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudCambioActividadAdminService adminService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listarSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/solicitudes-cambio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarConRolPublicadorDevuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/solicitudes-cambio")
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprobarConRolAdminDevuelveDetalle() throws Exception {
        SolicitudCambioDetalleDTO detalle = new SolicitudCambioDetalleDTO();
        detalle.setId(80L);
        detalle.setEstado("APROBADA");

        when(adminService.aprobar(eq(80L), eq(50L))).thenReturn(detalle);

        mockMvc.perform(post("/api/admin/solicitudes-cambio/80/aprobar")
                        .with(jwtConRol("ADMIN", 50L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(80))
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void actualizarEstadoSinEstadoDevuelveBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/solicitudes-cambio/80/estado")
                        .with(jwtConRol("ADMIN", 50L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.estado").exists());
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
