package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo del cambio de password (fase 5a) contra PostgreSQL
 * local: el cambio mata las sesiones ajenas, la sesion devuelta sigue
 * viva, el login queda en la password nueva, la actual incorrecta no
 * revoca nada, y el freno de fuerza bruta corta en el sexto intento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-integration-local-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class CambiarPasswordIT {

    private static final String PASSWORD_ORIGINAL = "PasswordOriginal123!";
    private static final String PASSWORD_NUEVA = "PasswordNueva456!";

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
    private PasswordEncoder passwordEncoder;

    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void verificarDatasourceLocal() {
        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    /* La FK de refresh_token es ON DELETE CASCADE: borrar el usuario alcanza. */
    @AfterEach
    void limpiarUsuariosCreadosPorElTest() {
        for (Long userId : createdUserIds) {
            usuarioRepository.findById(userId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();
        createdUserIds.clear();
    }

    @Test
    void elCambioMataLasSesionesAjenasYConservaLaDevuelta() throws Exception {
        Usuario usuario = crearUsuario();

        /* Dos "dispositivos": dos logins, dos familias de refresh. */
        JsonNode dispositivoA = login(usuario.getEmail(), PASSWORD_ORIGINAL);
        JsonNode dispositivoB = login(usuario.getEmail(), PASSWORD_ORIGINAL);
        String refreshB = dispositivoB.get("refreshToken").asText();

        String cuerpoCambio = mockMvc.perform(post("/api/auth/cambiar-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dispositivoA.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCambio(PASSWORD_ORIGINAL, PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sesionDelCambio = objectMapper.readTree(cuerpoCambio);
        assertNotEquals(dispositivoA.get("refreshToken").asText(), sesionDelCambio.get("refreshToken").asText());

        /* El otro dispositivo quedo afuera: su refresh esta revocado. */
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(refreshB)))
                .andExpect(status().isUnauthorized());

        /* La sesion devuelta por el cambio sigue viva: refresh y access sirven. */
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(sesionDelCambio.get("refreshToken").asText())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesionDelCambio.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));

        /* El login quedo en la password nueva; la vieja ya no entra. */
        login(usuario.getEmail(), PASSWORD_NUEVA);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(usuario.getEmail(), PASSWORD_ORIGINAL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laActualIncorrectaDa400YNoRevocaNada() throws Exception {
        Usuario usuario = crearUsuario();
        JsonNode sesion = login(usuario.getEmail(), PASSWORD_ORIGINAL);

        mockMvc.perform(post("/api/auth/cambiar-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCambio("Equivocada999!", PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La password actual no es correcta."));

        /* La sesion original sigue intacta y la password no cambio. */
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRefresh(sesion.get("refreshToken").asText())))
                .andExpect(status().isOk());
        login(usuario.getEmail(), PASSWORD_ORIGINAL);
    }

    @Test
    void elSextoIntentoFallidoDa429AunqueLaPasswordSeaCorrecta() throws Exception {
        Usuario usuario = crearUsuario();
        JsonNode sesion = login(usuario.getEmail(), PASSWORD_ORIGINAL);
        String access = sesion.get("accessToken").asText();

        for (int intento = 0; intento < 5; intento++) {
            mockMvc.perform(post("/api/auth/cambiar-password")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonCambio("Equivocada999!", PASSWORD_NUEVA, PASSWORD_NUEVA)))
                    .andExpect(status().isBadRequest());
        }

        /*
          Bloqueado es bloqueado: ni con la password correcta pasa. Sin
          esto el limitador seria un oraculo (429 = "erraste", 200 =
          "acertaste").
        */
        mockMvc.perform(post("/api/auth/cambiar-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCambio(PASSWORD_ORIGINAL, PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void anonimoDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/cambiar-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCambio(PASSWORD_ORIGINAL, PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode login(String email, String password) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(cuerpo);
    }

    private String jsonLogin(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String jsonRefresh(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }

    private String jsonCambio(String actual, String nueva, String confirmar) {
        return """
                {
                  "passwordActual": "%s",
                  "passwordNueva": "%s",
                  "confirmarPassword": "%s"
                }
                """.formatted(actual, nueva, confirmar);
    }

    private Usuario crearUsuario() {
        Rol rol = rolRepository.findByNombre("USUARIO")
                .orElseThrow(() -> new IllegalStateException("No existe el rol requerido: USUARIO"));
        String email = "cambiar-password-it-" + UUID.randomUUID() + "@dondeentreno.test";
        OffsetDateTime ahora = OffsetDateTime.now();

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("CambiarPassword IT");
        usuario.setApellido("Usuario");
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD_ORIGINAL));
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
