package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.RefreshToken;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.RefreshTokenRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo del refresh token contra PostgreSQL local (script 19):
 * login emite, refresh rota, el reuso revoca la familia, el logout
 * revoca en el servidor, y vencido/revocado no sirven.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-integration-local-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class RefreshTokenIT {

    private static final String PASSWORD_TEST = "PasswordTestRefresh123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void verificarDatasourceLocal() {
        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    /*
      Borrar el usuario alcanza: la FK de refresh_token es ON DELETE
      CASCADE (una sesion es del usuario, no un dato propio).
    */
    @AfterEach
    void limpiarUsuariosCreadosPorElTest() {
        for (Long userId : createdUserIds) {
            usuarioRepository.findById(userId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();
        createdUserIds.clear();
    }

    @Test
    void loginEmiteRefreshYElRefreshRotaUnaSesionCompletaUsable() throws Exception {
        Usuario usuario = crearUsuario("USUARIO");

        JsonNode sesionInicial = login(usuario.getEmail());
        String refreshInicial = sesionInicial.get("refreshToken").asText();

        /* 32 bytes en base64url sin padding = 43 chars. */
        assertEquals(43, refreshInicial.length());
        assertEquals(30L * 24 * 60 * 60, sesionInicial.get("refreshExpiresIn").asLong());

        String cuerpoRefresh = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refreshInicial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sesionRotada = objectMapper.readTree(cuerpoRefresh);
        String refreshNuevo = sesionRotada.get("refreshToken").asText();
        String accessNuevo = sesionRotada.get("accessToken").asText();

        assertNotEquals(refreshInicial, refreshNuevo);

        /* El access nuevo autentica de verdad. */
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessNuevo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));

        /* Y ambos tokens comparten familia en la base. */
        RefreshToken filaInicial = filaDe(refreshInicial);
        RefreshToken filaNueva = filaDe(refreshNuevo);
        assertEquals(filaInicial.getFamilia(), filaNueva.getFamilia());
        assertNotNull(filaInicial.getUsadoEn());
    }

    @Test
    void reusarUnRefreshViejoFueraDeLaGraciaRevocaLaFamiliaEntera() throws Exception {
        Usuario usuario = crearUsuario("USUARIO");

        String refreshInicial = login(usuario.getEmail()).get("refreshToken").asText();

        String refreshNuevo = objectMapper.readTree(
                mockMvc.perform(post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRefresh(refreshInicial)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        ).get("refreshToken").asText();

        /*
          El reuso inmediato cae en la gracia anti-carrera de pestañas,
          asi que el "robo" se simula envejeciendo el usado_en en la
          base, como si el token robado se usara minutos despues.
        */
        RefreshToken filaInicial = filaDe(refreshInicial);
        filaInicial.setUsadoEn(OffsetDateTime.now().minusMinutes(5));
        refreshTokenRepository.saveAndFlush(filaInicial);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refreshInicial)))
                .andExpect(status().isUnauthorized());

        /* La familia cayo completa: el token "legitimo" tampoco sirve ya. */
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refreshNuevo)))
                .andExpect(status().isUnauthorized());

        assertNotNull(filaDe(refreshNuevo).getRevocadoEn());
    }

    @Test
    void elLogoutRevocaEnElServidorYEsIdempotente() throws Exception {
        Usuario usuario = crearUsuario("USUARIO");

        String refresh = login(usuario.getEmail()).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refresh)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refresh)))
                .andExpect(status().isUnauthorized());

        /* Repetir el logout no falla ni delata nada. */
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refresh)))
                .andExpect(status().isNoContent());
    }

    @Test
    void unRefreshVencidoDevuelve401() throws Exception {
        Usuario usuario = crearUsuario("USUARIO");

        String refresh = login(usuario.getEmail()).get("refreshToken").asText();

        RefreshToken fila = filaDe(refresh);
        fila.setExpiraEn(OffsetDateTime.now().minusMinutes(1));
        refreshTokenRepository.saveAndFlush(fila);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unRefreshInventadoDevuelve401ConMensajeGenerico() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh("token-que-no-existe-para-nada-1234567890x")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Sesion invalida o vencida."));
    }

    private JsonNode login(String email) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD_TEST)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(cuerpo);
    }

    private String jsonRefresh(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }

    /*
      El test localiza la fila igual que el servicio: hasheando el token
      en claro. Si esto dejara de matchear, el diseño "solo hashes en la
      base" se habria roto y este helper lo delataria.
    */
    private RefreshToken filaDe(String tokenPlano) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hash = HexFormat.of().formatHex(
                digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8))
        );

        return refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalStateException("No existe la fila del refresh token esperado."));
    }

    private Usuario crearUsuario(String nombreRol) {
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("No existe el rol requerido: " + nombreRol));
        String email = "refresh-it-" + UUID.randomUUID() + "@dondeentreno.test";
        OffsetDateTime ahora = OffsetDateTime.now();

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Refresh IT");
        usuario.setApellido(nombreRol);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD_TEST));
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        createdUserIds.add(guardado.getId());
        return guardado;
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        String urlNormalizada = url.toLowerCase();
        return urlNormalizada.contains("supabase") || urlNormalizada.contains("render") || urlNormalizada.contains("pooler");
    }
}
