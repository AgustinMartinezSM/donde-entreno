package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.PerfilPublicadorActualDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorDetalleDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorHorarioDTO;
import com.dondeentreno.api.dto.SolicitudPublicadorResumenDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.security.CustomUserDetailsService;
import com.dondeentreno.api.security.RestAccessDeniedHandler;
import com.dondeentreno.api.security.RestAuthenticationEntryPoint;
import com.dondeentreno.api.security.SecurityConfig;
import com.dondeentreno.api.service.PublicadorService;
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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicadorController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "dondeentreno.auth.jwt.secret=clave-ficticia-de-test-con-longitud-suficiente-123456")
class PublicadorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicadorService publicadorService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void obtenerMiPerfilSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/publicador/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."));
    }

    @Test
    void obtenerMiPerfilConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/publicador/me")
                        .with(jwtConRol("USUARIO", 10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void obtenerMiPerfilConRolPublicadorDevuelvePerfil() throws Exception {
        when(publicadorService.obtenerMiPerfil(20L))
                .thenReturn(new PerfilPublicadorActualDTO(
                        30L,
                        "Perfil Publicador",
                        "PROFESOR_INDEPENDIENTE",
                        "PENDIENTE_REVISION",
                        1L,
                        "Mar del Plata",
                        "+54 223 555 9999",
                        "@perfil",
                        "contacto@ejemplo.com",
                        "+54 223 555 8888",
                        "Descripcion",
                        true,
                        false
                ));

        mockMvc.perform(get("/api/publicador/me")
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.nombre").value("Perfil Publicador"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.ciudadPrincipalId").value(1))
                .andExpect(jsonPath("$.verificado").value(false));
    }

    @Test
    void obtenerMiPerfilSinPerfilDevuelveNotFoundControlado() throws Exception {
        when(publicadorService.obtenerMiPerfil(20L))
                .thenThrow(new RecursoNoEncontradoException(
                        "No se encontro un perfil publicador para el usuario autenticado."
                ));

        mockMvc.perform(get("/api/publicador/me")
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje")
                        .value("No se encontro un perfil publicador para el usuario autenticado."));
    }

    @Test
    void listarSolicitudesSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/publicador/solicitudes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void listarSolicitudesConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/publicador/solicitudes")
                        .with(jwtConRol("USUARIO", 10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listarSolicitudesConRolPublicadorDevuelvePagina() throws Exception {
        when(publicadorService.listarMisSolicitudes(20L, "pendiente", 0, 20, "recientes"))
                .thenReturn(new PaginaResponseDTO<>(
                        List.of(new SolicitudPublicadorResumenDTO(
                                100L,
                                "DEP-20260704-ABC12345",
                                "PENDIENTE",
                                "Boxeo recreativo",
                                1L,
                                "Boxeo",
                                null,
                                2L,
                                "Mar del Plata",
                                null,
                                3L,
                                "Centro",
                                null,
                                OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                                OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                                null,
                                null,
                                null
                        )),
                        0,
                        20,
                        1,
                        1,
                        true
                ));

        mockMvc.perform(get("/api/publicador/solicitudes")
                        .param("estado", "pendiente")
                        .param("page", "0")
                        .param("size", "20")
                        .param("orden", "recientes")
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(100))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(publicadorService).listarMisSolicitudes(20L, "pendiente", 0, 20, "recientes");
    }

    @Test
    void obtenerSolicitudConRolPublicadorDevuelveDetalle() throws Exception {
        when(publicadorService.obtenerMiSolicitud(20L, 100L))
                .thenReturn(detalleSolicitud());

        mockMvc.perform(get("/api/publicador/solicitudes/{id}", 100L)
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.horarios[0].diaSemana").value("LUNES"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(publicadorService).obtenerMiSolicitud(20L, 100L);
    }

    @Test
    void obtenerSolicitudInexistenteDevuelveNotFound() throws Exception {
        when(publicadorService.obtenerMiSolicitud(20L, 999L))
                .thenThrow(new RecursoNoEncontradoException("Solicitud no encontrada."));

        mockMvc.perform(get("/api/publicador/solicitudes/{id}", 999L)
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Solicitud no encontrada."));
    }

    @Test
    void crearSolicitudConRolPublicadorDevuelveCreated() throws Exception {
        when(publicadorService.crearMiSolicitud(eq(20L), any()))
                .thenReturn(new SolicitudPublicacionResponseDTO(
                        100L,
                        "DEP-20260704-ABC12345",
                        "PENDIENTE",
                        OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                        "La solicitud fue recibida correctamente y quedo pendiente de revision."
                ));

        mockMvc.perform(post("/api/publicador/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitudPublicadorValida())
                        .with(jwtConRol("PUBLICADOR", 20L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(publicadorService).crearMiSolicitud(eq(20L), any());
    }

    @Test
    void crearSolicitudConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(post("/api/publicador/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitudPublicadorValida())
                        .with(jwtConRol("USUARIO", 10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void crearSolicitudSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(post("/api/publicador/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitudPublicadorValida()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private SolicitudPublicadorDetalleDTO detalleSolicitud() {
        return new SolicitudPublicadorDetalleDTO(
                100L,
                "DEP-20260704-ABC12345",
                "PENDIENTE",
                "Boxeo recreativo",
                1L,
                "Boxeo",
                null,
                2L,
                "Mar del Plata",
                null,
                3L,
                "Centro",
                null,
                OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                OffsetDateTime.parse("2026-07-04T10:00:00-03:00"),
                null,
                null,
                "Clases para principiantes.",
                "PRINCIPIANTE",
                "RECREATIVO",
                "PRESENCIAL",
                18,
                60,
                null,
                false,
                "Escuela Norte",
                "Av. Independencia 1234",
                null,
                "+54 9 223 512-3456",
                "@escuelanorte",
                "contacto@ejemplo.com",
                null,
                null,
                null,
                List.of(new SolicitudPublicadorHorarioDTO(
                        50L,
                        "LUNES",
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 30),
                        null
                ))
        );
    }

    private String jsonSolicitudPublicadorValida() {
        return """
                {
                  "nombreActividad": "Boxeo recreativo",
                  "deporteId": 1,
                  "deporteOtro": null,
                  "descripcion": "Clases para principiantes.",
                  "nivel": "PRINCIPIANTE",
                  "enfoque": "RECREATIVO",
                  "modalidad": "PRESENCIAL",
                  "edadMinima": 18,
                  "edadMaxima": 60,
                  "precioReferencia": 18000.00,
                  "mostrarPrecio": true,
                  "ciudadId": 1,
                  "ciudadOtra": null,
                  "barrioId": 1,
                  "barrioOtro": null,
                  "nombreLugar": "Escuela Norte",
                  "direccion": "Av. Independencia 1234",
                  "referenciaUbicacion": null,
                  "whatsapp": "+54 9 223 512-3456",
                  "instagram": "@escuelanorte",
                  "email": "contacto@ejemplo.com",
                  "observacionesSolicitante": null,
                  "aceptaCondiciones": true,
                  "horarios": [
                    {
                      "diaSemana": "LUNES",
                      "horaInicio": "18:00",
                      "horaFin": "19:30",
                      "observacion": null
                    }
                  ]
                }
                """;
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
