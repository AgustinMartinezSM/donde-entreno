package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminHorarioDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionAdminResumenDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionCambiarEstadoRequestDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.SolicitudPublicacionInvalidaException;
import com.dondeentreno.api.security.CustomUserDetailsService;
import com.dondeentreno.api.security.RestAccessDeniedHandler;
import com.dondeentreno.api.security.RestAuthenticationEntryPoint;
import com.dondeentreno.api.security.SecurityConfig;
import com.dondeentreno.api.service.SolicitudPublicacionAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SolicitudPublicacionAdminController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "dondeentreno.auth.jwt.secret=clave-ficticia-de-test-con-longitud-suficiente-123456")
class SolicitudPublicacionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudPublicacionAdminService solicitudPublicacionAdminService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getListadoSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/solicitudes-publicacion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."));
    }

    @Test
    void getListadoConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/solicitudes-publicacion")
                        .with(jwtConRol("USUARIO", 123L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getListadoConSuperAdminDevuelvePagina() throws Exception {
        when(solicitudPublicacionAdminService.listarSolicitudes("PENDIENTE", 0, 10, "recientes"))
                .thenReturn(new PaginaResponseDTO<>(
                        List.of(resumen()),
                        0,
                        10,
                        1,
                        1,
                        true
                ));

        mockMvc.perform(get("/api/admin/solicitudes-publicacion")
                        .param("estado", "PENDIENTE")
                        .param("page", "0")
                        .param("size", "10")
                        .param("orden", "recientes")
                        .with(jwtConRol("SUPER_ADMIN", 123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(10))
                .andExpect(jsonPath("$.contenido[0].codigoSeguimiento").value("DEP-20260630-ABC12345"))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.paginaActual").value(0))
                .andExpect(jsonPath("$.tamanioPagina").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void getListadoConAdminDevuelveOk() throws Exception {
        when(solicitudPublicacionAdminService.listarSolicitudes(null, 0, 10, "recientes"))
                .thenReturn(new PaginaResponseDTO<>(List.of(), 0, 10, 0, 0, true));

        mockMvc.perform(get("/api/admin/solicitudes-publicacion")
                        .with(jwtConRol("ADMIN", 123L)))
                .andExpect(status().isOk());

        verify(solicitudPublicacionAdminService).listarSolicitudes(null, 0, 10, "recientes");
    }

    @Test
    void getDetalleConSuperAdminDevuelveDetalleConHorarios() throws Exception {
        when(solicitudPublicacionAdminService.obtenerDetalle(10L)).thenReturn(detalle("PENDIENTE"));

        mockMvc.perform(get("/api/admin/solicitudes-publicacion/10")
                        .with(jwtConRol("SUPER_ADMIN", 123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.horarios[0].diaSemana").value("LUNES"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.whatsappNormalizado").doesNotExist())
                .andExpect(jsonPath("$.ipOrigen").doesNotExist());
    }

    @Test
    void getDetalleInexistenteDevuelveNotFound() throws Exception {
        when(solicitudPublicacionAdminService.obtenerDetalle(99L))
                .thenThrow(new RecursoNoEncontradoException("Solicitud de publicacion no encontrada."));

        mockMvc.perform(get("/api/admin/solicitudes-publicacion/99")
                        .with(jwtConRol("SUPER_ADMIN", 123L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Solicitud de publicacion no encontrada."));
    }

    @Test
    void patchEnRevisionConSuperAdminDevuelveDetalleYEnviaUserIdAlService() throws Exception {
        when(solicitudPublicacionAdminService.cambiarEstado(eq(10L), any(), eq(123L)))
                .thenReturn(detalle("EN_REVISION"));

        mockMvc.perform(patch("/api/admin/solicitudes-publicacion/10/estado")
                        .with(jwtConRol("SUPER_ADMIN", 123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "EN_REVISION",
                                  "motivoRechazo": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));

        ArgumentCaptor<SolicitudPublicacionCambiarEstadoRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(SolicitudPublicacionCambiarEstadoRequestDTO.class);
        verify(solicitudPublicacionAdminService).cambiarEstado(eq(10L), requestCaptor.capture(), eq(123L));
        assertEquals("EN_REVISION", requestCaptor.getValue().getEstado());
    }

    @Test
    void patchRechazadaConSuperAdminDevuelveDetalle() throws Exception {
        when(solicitudPublicacionAdminService.cambiarEstado(eq(10L), any(), eq(123L)))
                .thenReturn(detalle("RECHAZADA"));

        mockMvc.perform(patch("/api/admin/solicitudes-publicacion/10/estado")
                        .with(jwtConRol("SUPER_ADMIN", 123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "RECHAZADA",
                                  "motivoRechazo": "Falta informacion de horarios"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    void patchAprobadaDevuelveBadRequestYNoLlamaService() throws Exception {
        mockMvc.perform(patch("/api/admin/solicitudes-publicacion/10/estado")
                        .with(jwtConRol("SUPER_ADMIN", 123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "APROBADA",
                                  "motivoRechazo": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.estado").exists());

        verify(solicitudPublicacionAdminService, never()).cambiarEstado(any(), any(), any());
    }

    @Test
    void patchRechazadaSinMotivoDevuelveBadRequestDelService() throws Exception {
        when(solicitudPublicacionAdminService.cambiarEstado(eq(10L), any(), eq(123L)))
                .thenThrow(new SolicitudPublicacionInvalidaException("El motivo de rechazo es obligatorio."));

        mockMvc.perform(patch("/api/admin/solicitudes-publicacion/10/estado")
                        .with(jwtConRol("SUPER_ADMIN", 123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "RECHAZADA",
                                  "motivoRechazo": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El motivo de rechazo es obligatorio."));
    }

    @Test
    void patchSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/admin/solicitudes-publicacion/10/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "EN_REVISION",
                                  "motivoRechazo": null
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/solicitudes-publicacion/10/estado")
                        .with(jwtConRol("USUARIO", 123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "EN_REVISION",
                                  "motivoRechazo": null
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("admin@example.com")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private SolicitudPublicacionAdminResumenDTO resumen() {
        return new SolicitudPublicacionAdminResumenDTO(
                10L,
                "DEP-20260630-ABC12345",
                "PENDIENTE",
                "FORMULARIO_WEB",
                "ESCUELA_DEPORTIVA",
                "Escuela Norte",
                "Boxeo recreativo",
                1L,
                "Boxeo",
                null,
                1L,
                "Mar del Plata",
                null,
                1L,
                "Centro",
                null,
                "contacto@example.com",
                "+54 9 223 512-3456",
                OffsetDateTime.parse("2026-06-30T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-30T10:00:00-03:00"),
                null,
                null
        );
    }

    private SolicitudPublicacionAdminDetalleDTO detalle(String estado) {
        return new SolicitudPublicacionAdminDetalleDTO(
                10L,
                "DEP-20260630-ABC12345",
                estado,
                "FORMULARIO_WEB",
                "ESCUELA_DEPORTIVA",
                "Escuela Norte",
                "Boxeo recreativo",
                1L,
                "Boxeo",
                null,
                1L,
                "Mar del Plata",
                null,
                1L,
                "Centro",
                null,
                "contacto@example.com",
                "+54 9 223 512-3456",
                OffsetDateTime.parse("2026-06-30T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-30T10:00:00-03:00"),
                null,
                null,
                "Clases para principiantes.",
                "PRINCIPIANTE",
                "RECREATIVO",
                "PRESENCIAL",
                18,
                60,
                null,
                true,
                "Escuela Norte",
                "Av. Independencia 1234",
                null,
                "@escuelanorte",
                null,
                null,
                null,
                null,
                null,
                List.of(new SolicitudPublicacionAdminHorarioDTO(
                        1L,
                        "LUNES",
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 30),
                        null
                ))
        );
    }
}
