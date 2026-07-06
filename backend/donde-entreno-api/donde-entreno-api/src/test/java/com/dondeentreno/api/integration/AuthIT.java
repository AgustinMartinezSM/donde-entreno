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
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-integration-local-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class AuthIT {

    private static final String PASSWORD_TEST = "PasswordTestAuth123!";

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
    private final List<String> createdEmails = new ArrayList<>();

    @BeforeEach
    void verificarDatasourceLocal() {
        verificarVariablesLocalesPresentes();

        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @AfterEach
    void limpiarUsuariosCreadosPorElTest() {
        for (Long userId : createdUserIds) {
            usuarioRepository.findById(userId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();

        for (String email : createdEmails) {
            assertTrue(usuarioRepository.findByEmailNormalizado(normalizarEmail(email)).isEmpty());
        }

        createdUserIds.clear();
        createdEmails.clear();
    }

    @Test
    void loginExitosoContraUsuarioPersistidoDevuelveJwtUsable() throws Exception {
        Usuario usuario = crearUsuario("ADMIN", true, true);
        String emailConMayusculas = usuario.getEmail().toUpperCase(Locale.ROOT);

        String token = loginYObtenerToken(emailConMayusculas, PASSWORD_TEST);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(emailConMayusculas, PASSWORD_TEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.accessToken").value(startsWith("ey")))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void loginConPasswordIncorrectaDevuelveUnauthorizedGenerico() throws Exception {
        Usuario usuario = crearUsuario("ADMIN", true, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(usuario.getEmail(), "PasswordIncorrecta123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Email o password invalidos."))
                .andExpect(jsonPath("$.mensaje", not("Usuario inactivo.")))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void usuariosInactivoYNoVerificadoNoPuedenLoguearse() throws Exception {
        Usuario usuarioInactivo = crearUsuario("ADMIN", false, true);
        Usuario usuarioNoVerificado = crearUsuario("ADMIN", true, false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(usuarioInactivo.getEmail(), PASSWORD_TEST)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o password invalidos."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(usuarioNoVerificado.getEmail(), PASSWORD_TEST)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o password invalidos."));
    }

    @Test
    void adminSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."));
    }

    @Test
    void adminConRolUsuarioDevuelveForbidden() throws Exception {
        Usuario usuario = crearUsuario("USUARIO", true, true);
        String token = loginYObtenerToken(usuario.getEmail(), PASSWORD_TEST);

        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value("No tenes permisos para acceder a este recurso."));
    }

    @Test
    void adminConRolAdminOSuperAdminPasaLaCapaDeSeguridad() throws Exception {
        for (String rol : List.of("ADMIN", "SUPER_ADMIN")) {
            Usuario usuario = crearUsuario(rol, true, true);
            String token = loginYObtenerToken(usuario.getEmail(), PASSWORD_TEST);

            mockMvc.perform(get("/api/admin/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                    .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
        }
    }

    @Test
    void adminConTokenInvalidoDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("No autenticado."));
    }

    private Usuario crearUsuario(String nombreRol, boolean activo, boolean emailVerificado) {
        verificarDatasourceLocal();

        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("No existe el rol requerido para AuthIT: " + nombreRol));
        String email = "auth-it-" + UUID.randomUUID() + "@dondeentreno.test";
        String passwordHash = passwordEncoder.encode(PASSWORD_TEST);
        OffsetDateTime ahora = OffsetDateTime.now();

        assertTrue(passwordEncoder.matches(PASSWORD_TEST, passwordHash));
        assertNotEquals(PASSWORD_TEST, passwordHash);

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Auth IT");
        usuario.setApellido(nombreRol);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordHash);
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(activo);
        usuario.setEmailVerificado(emailVerificado);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        createdUserIds.add(guardado.getId());
        createdEmails.add(guardado.getEmail());
        return guardado;
    }

    private String loginYObtenerToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private String jsonLogin(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private void verificarVariablesLocalesPresentes() {
        assertFalse(
                System.getenv("DONDEENTRENO_TEST_DB_USERNAME") == null
                        || System.getenv("DONDEENTRENO_TEST_DB_USERNAME").isBlank(),
                "Debe existir DONDEENTRENO_TEST_DB_USERNAME para integration-local."
        );
        assertFalse(
                System.getenv("DONDEENTRENO_TEST_DB_PASSWORD") == null
                        || System.getenv("DONDEENTRENO_TEST_DB_PASSWORD").isBlank(),
                "Debe existir DONDEENTRENO_TEST_DB_PASSWORD para integration-local."
        );
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        return url.toLowerCase(Locale.ROOT).matches(
                ".*(supabase|render|amazonaws|azure|neon|railway|aiven|digitalocean|\\.com|\\.net|\\.io|\\.app).*"
        );
    }
}
