package com.dondeentreno.api.security;

import com.dondeentreno.api.controller.AuthController;
import com.dondeentreno.api.controller.HealthController;
import com.dondeentreno.api.controller.SolicitudPublicacionController;
import com.dondeentreno.api.dto.AuthUsuarioDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.dto.SolicitudPublicacionResponseDTO;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.service.AuthService;
import com.dondeentreno.api.service.SolicitudPublicacionService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        HealthController.class,
        SolicitudPublicacionController.class,
        AuthController.class
}, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=clave-ficticia-de-test-con-longitud-suficiente-123456",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api"
})
class SecurityConfigTest {

    private static final String JWT_SECRET_TEST = "clave-ficticia-de-test-con-longitud-suficiente-123456";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudPublicacionService solicitudPublicacionService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void healthSiguePublico() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void solicitudPublicacionNoQuedaBloqueadaPorSecurityOCrf() throws Exception {
        when(solicitudPublicacionService.crearSolicitud(any()))
                .thenReturn(new SolicitudPublicacionResponseDTO(
                        1L,
                        "DEP-20260630-ABC12345",
                        "PENDIENTE",
                        OffsetDateTime.parse("2026-06-30T10:00:00-03:00"),
                        "La solicitud fue recibida correctamente y quedo pendiente de revision."
                ));

        mockMvc.perform(post("/api/solicitudes-publicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitudValida()))
                .andExpect(status().isCreated());
    }

    @Test
    void authLoginSiguePublico() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResponseDTO(
                        "Bearer",
                        "token-ficticio",
                        3600L,
                        new AuthUsuarioDTO(1L, "admin@dondeentreno.com", "Admin", null, "SUPER_ADMIN")
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@dondeentreno.com",
                                  "password": "password-ficticio"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void optionsNoQuedaBloqueadoPorSecurity() throws Exception {
        mockMvc.perform(options("/api/admin/test"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void adminSinAutenticacionDevuelveUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."))
                .andExpect(jsonPath("$.path").value("/api/admin/test"));
    }

    @Test
    void adminConJwtInvalidoDevuelveUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."))
                .andExpect(jsonPath("$.path").value("/api/admin/test"));
    }

    @Test
    void adminConJwtRolUsuarioDevuelveForbiddenJson() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenConRol("USUARIO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensaje").value("No tenes permisos para acceder a este recurso."))
                .andExpect(jsonPath("$.path").value("/api/admin/test"));
    }

    @Test
    void adminConJwtRolAdminPasaLaCapaDeSecurity() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenConRol("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Recurso no encontrado."))
                .andExpect(jsonPath("$.path").value("/api/admin/test"));
    }

    @Test
    void adminConJwtRolSuperAdminPasaLaCapaDeSecurity() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenConRol("SUPER_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Recurso no encontrado."))
                .andExpect(jsonPath("$.path").value("/api/admin/test"));
    }

    @Test
    void rutaPublicaInexistenteDevuelveNotFoundJson() throws Exception {
        mockMvc.perform(get("/api/actividades/ruta-inexistente-publica"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Recurso no encontrado."))
                .andExpect(jsonPath("$.path").value("/api/actividades/ruta-inexistente-publica"));
    }

    @Test
    void ciudadPorSlugNoQuedaBloqueadaPorSecurity() throws Exception {
        mockMvc.perform(get("/api/ciudades/mar-del-plata"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Recurso no encontrado."))
                .andExpect(jsonPath("$.path").value("/api/ciudades/mar-del-plata"));
    }

    private String bearerTokenConRol(String rol) {
        SecretKeySpec secretKey = new SecretKeySpec(
                JWT_SECRET_TEST.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dondeentreno-api")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .subject("usuario@dondeentreno.com")
                .claim("userId", 1L)
                .claim("rol", rol)
                .claim("roles", List.of(rol))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
                claims
        )).getTokenValue();

        return "Bearer " + token;
    }

    private String jsonSolicitudValida() {
        return """
                {
                  "tipoPublicador": "ESCUELA_DEPORTIVA",
                  "nombrePublicador": "Escuela de Boxeo Norte",
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
                  "email": "CONTACTO@EJEMPLO.COM",
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
}
