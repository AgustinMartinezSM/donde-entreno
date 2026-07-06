package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
class PublicadorFlowIT {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ORIGEN_FORMULARIO_WEB = "FORMULARIO_WEB";
    private static final String PASSWORD_TEST = "PasswordPublicador123!";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String ROL_USUARIO = "USUARIO";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Autowired
    private SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    private final List<Long> solicitudIds = new ArrayList<>();
    private final List<Long> solicitudHorarioIds = new ArrayList<>();
    private final List<Long> perfilPublicadorIds = new ArrayList<>();
    private final List<Long> usuarioIds = new ArrayList<>();
    private final List<String> marcadores = new ArrayList<>();

    @BeforeEach
    void verificarDatasourceLocal() {
        verificarVariablesLocalesPresentes();

        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        verificarDatasourceLocal();
        registrarResiduosPorMarcador();
        registrarHorariosDeSolicitudes();

        for (Long solicitudHorarioId : solicitudHorarioIds) {
            solicitudPublicacionHorarioRepository.findById(solicitudHorarioId)
                    .ifPresent(solicitudPublicacionHorarioRepository::delete);
        }
        solicitudPublicacionHorarioRepository.flush();

        for (Long solicitudId : solicitudIds) {
            solicitudPublicacionRepository.findById(solicitudId).ifPresent(solicitudPublicacionRepository::delete);
        }
        solicitudPublicacionRepository.flush();

        for (Long perfilPublicadorId : perfilPublicadorIds) {
            perfilPublicadorRepository.findById(perfilPublicadorId).ifPresent(perfilPublicadorRepository::delete);
        }
        perfilPublicadorRepository.flush();

        for (Long usuarioId : usuarioIds) {
            usuarioRepository.findById(usuarioId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();

        for (String marcador : marcadores) {
            assertFalse(existeResiduoConMarcador(marcador), "Quedaron datos residuales del test: " + marcador);
        }

        solicitudIds.clear();
        solicitudHorarioIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
        marcadores.clear();
    }

    @Test
    void registroUsuarioLoginYMePersistenDatosNormalizadosSinExponerPassword() throws Exception {
        String marcador = marcadorUnico();
        String email = "usuario-" + marcador + "@dondeentreno.test";

        JsonNode registro = registrarUsuario(marcador, email);

        Long usuarioId = registro.at("/usuario/id").asLong();
        usuarioIds.add(usuarioId);

        assertEquals(ROL_USUARIO, registro.at("/usuario/rol").asText());
        assertFalse(registro.at("/usuario").has("passwordHash"));
        assertTrue(registro.hasNonNull("accessToken"));

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        assertTrue(usuarioRepository.findByEmailNormalizado(normalizarEmail(email)).isPresent());
        assertNotNull(usuario.getTelefonoNormalizado());
        assertFalse(usuario.getTelefonoNormalizado().isBlank());
        assertEquals(Boolean.FALSE, usuario.getTelefonoVerificado());

        String token = loginYObtenerToken(email.toUpperCase(Locale.ROOT), PASSWORD_TEST);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId))
                .andExpect(jsonPath("$.email").value(normalizarEmail(email)))
                .andExpect(jsonPath("$.rol").value(ROL_USUARIO))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registroPublicadorPerfilSolicitudPropiaYAislamientoEntrePublicadores() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        Publicador publicador = registrarPublicador(marcadorUnico(), referencias.ciudad());
        Publicador otroPublicador = registrarPublicador(marcadorUnico(), referencias.ciudad());
        String token = loginYObtenerToken(publicador.email(), PASSWORD_TEST);
        String tokenOtroPublicador = loginYObtenerToken(otroPublicador.email(), PASSWORD_TEST);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicador.usuario().getId()))
                .andExpect(jsonPath("$.rol").value(ROL_PUBLICADOR))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/publicador/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicador.perfil().getId()))
                .andExpect(jsonPath("$.estado").value(ESTADO_PERFIL_PENDIENTE_REVISION))
                .andExpect(jsonPath("$.ciudadPrincipalId").value(referencias.ciudad().getId()))
                .andExpect(jsonPath("$.whatsapp").value(publicador.whatsapp()));

        String marcadorSolicitud = marcadorUnico();
        Long solicitudId = crearSolicitudPublicador(token, referencias, marcadorSolicitud, publicador.email());
        Long solicitudOtroPublicador = crearSolicitudPublicador(
                tokenOtroPublicador,
                referencias,
                marcadorUnico(),
                otroPublicador.email()
        );

        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(solicitudId).orElseThrow();
        List<SolicitudPublicacionHorario> horarios =
                solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_IdOrderByDiaSemanaAscHoraInicioAsc(solicitudId);

        assertEquals(ESTADO_PENDIENTE, solicitud.getEstado());
        assertEquals(ORIGEN_FORMULARIO_WEB, solicitud.getOrigen());
        assertEquals(publicador.usuario().getId(), solicitud.getUsuario().getId());
        assertEquals(publicador.perfil().getId(), solicitud.getPerfilPublicador().getId());
        assertNull(solicitud.getActividadGenerada());
        assertEquals(2, horarios.size());

        JsonNode listado = leerJson(mockMvc.perform(get("/api/publicador/solicitudes")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()));
        assertTrue(contenidoTieneId(listado, solicitudId));
        assertFalse(contenidoTieneId(listado, solicitudOtroPublicador));

        mockMvc.perform(get("/api/publicador/solicitudes/{id}", solicitudId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId))
                .andExpect(jsonPath("$.nombreActividad").value("Actividad " + marcadorSolicitud))
                .andExpect(jsonPath("$.horarios.length()").value(2));

        mockMvc.perform(get("/api/publicador/solicitudes/{id}", solicitudOtroPublicador)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void solicitudPublicaAnonimaSigueSinUsuarioNiPerfilPublicador() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();

        Long solicitudId = crearSolicitudPublicaAnonima(referencias, marcador);

        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(solicitudId).orElseThrow();
        assertEquals(ESTADO_PENDIENTE, solicitud.getEstado());
        assertEquals(ORIGEN_FORMULARIO_WEB, solicitud.getOrigen());
        assertNull(solicitud.getUsuario());
        assertNull(solicitud.getPerfilPublicador());
        assertNull(solicitud.getActividadGenerada());
    }

    @Test
    void adminPuedeListarSolicitudesAnonimasYDePublicadorSinHacerlasPublicas() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        Publicador publicador = registrarPublicador(marcadorUnico(), referencias.ciudad());
        String token = loginYObtenerToken(publicador.email(), PASSWORD_TEST);
        Long solicitudPublicador = crearSolicitudPublicador(token, referencias, marcadorUnico(), publicador.email());
        Long solicitudAnonima = crearSolicitudPublicaAnonima(referencias, marcadorUnico());

        JsonNode listado = leerJson(mockMvc.perform(get("/api/admin/solicitudes-publicacion")
                        .param("size", "50")
                        .with(jwtConRol("SUPER_ADMIN", publicador.usuario().getId())))
                .andExpect(status().isOk()));

        assertTrue(contenidoTieneId(listado, solicitudPublicador));
        assertTrue(contenidoTieneId(listado, solicitudAnonima));
    }

    @Test
    void endpointsPublicadorRequierenRolPublicador() throws Exception {
        String marcador = marcadorUnico();
        String email = "seguridad-" + marcador + "@dondeentreno.test";

        registrarUsuario(marcador, email);
        Usuario usuario = usuarioRepository.findByEmailNormalizado(normalizarEmail(email)).orElseThrow();
        usuarioIds.add(usuario.getId());
        String tokenUsuario = loginYObtenerToken(email, PASSWORD_TEST);

        mockMvc.perform(get("/api/publicador/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/publicador/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/publicador/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private JsonNode registrarUsuario(String marcador, String email) throws Exception {
        return leerJson(mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistroUsuario(marcador, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist()));
    }

    private Publicador registrarPublicador(String marcador, Ciudad ciudad) throws Exception {
        String email = "publicador-" + marcador + "@dondeentreno.test";
        String whatsapp = "+54 9 223 000-0001";

        JsonNode registro = leerJson(mockMvc.perform(post("/api/auth/registro/publicador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistroPublicador(marcador, email, whatsapp, ciudad.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.usuario.rol").value(ROL_PUBLICADOR))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist()));

        Long usuarioId = registro.at("/usuario/id").asLong();
        usuarioIds.add(usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        PerfilPublicador perfil = perfilPublicadorRepository
                .findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(usuarioId)
                .orElseThrow();
        perfilPublicadorIds.add(perfil.getId());

        assertTrue(usuarioRepository.findByEmailNormalizado(normalizarEmail(email)).isPresent());
        assertEquals(Boolean.FALSE, usuario.getTelefonoVerificado());
        assertEquals(ESTADO_PERFIL_PENDIENTE_REVISION, perfil.getEstado());
        assertEquals(ciudad.getId(), perfil.getCiudadPrincipal().getId());
        assertNotNull(perfil.getWhatsappNormalizado());
        assertFalse(perfil.getWhatsappNormalizado().isBlank());
        assertNotNull(perfil.getTelefonoContactoNormalizado());
        assertFalse(perfil.getTelefonoContactoNormalizado().isBlank());

        return new Publicador(usuario, perfil, email, whatsapp);
    }

    private Long crearSolicitudPublicador(
            String token,
            Referencias referencias,
            String marcador,
            String emailContacto
    ) throws Exception {
        JsonNode response = leerJson(mockMvc.perform(post("/api/publicador/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitudPublicador(referencias, marcador, emailContacto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value(ESTADO_PENDIENTE))
                .andExpect(jsonPath("$.codigoSeguimiento", notNullValue())));

        Long solicitudId = response.get("id").asLong();
        solicitudIds.add(solicitudId);
        return solicitudId;
    }

    private Long crearSolicitudPublicaAnonima(Referencias referencias, String marcador) throws Exception {
        JsonNode response = leerJson(mockMvc.perform(post("/api/solicitudes-publicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitudPublica(referencias, marcador)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value(ESTADO_PENDIENTE))
                .andExpect(jsonPath("$.codigoSeguimiento", notNullValue())));

        Long solicitudId = response.get("id").asLong();
        solicitudIds.add(solicitudId);
        return solicitudId;
    }

    private String loginYObtenerToken(String email, String password) throws Exception {
        JsonNode response = leerJson(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist()));

        return response.get("accessToken").asText();
    }

    private JsonNode leerJson(org.springframework.test.web.servlet.ResultActions resultActions) throws Exception {
        String response = resultActions.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private void registrarHorariosDeSolicitudes() {
        for (Long solicitudId : solicitudIds) {
            solicitudPublicacionHorarioRepository.findBySolicitudPublicacion_Id(solicitudId).stream()
                    .map(SolicitudPublicacionHorario::getId)
                    .filter(id -> !solicitudHorarioIds.contains(id))
                    .forEach(solicitudHorarioIds::add);
        }
    }

    private void registrarResiduosPorMarcador() {
        for (String marcador : marcadores) {
            solicitudPublicacionRepository.findAll().stream()
                    .filter(solicitud -> contieneMarcador(solicitud.getNombreActividad(), marcador)
                            || contieneMarcador(solicitud.getNombrePublicador(), marcador)
                            || contieneMarcador(solicitud.getEmail(), marcador))
                    .map(SolicitudPublicacion::getId)
                    .filter(id -> !solicitudIds.contains(id))
                    .forEach(solicitudIds::add);

            perfilPublicadorRepository.findAll().stream()
                    .filter(perfil -> contieneMarcador(perfil.getNombre(), marcador)
                            || contieneMarcador(perfil.getEmailContacto(), marcador))
                    .map(PerfilPublicador::getId)
                    .filter(id -> !perfilPublicadorIds.contains(id))
                    .forEach(perfilPublicadorIds::add);

            usuarioRepository.findAll().stream()
                    .filter(usuario -> contieneMarcador(usuario.getNombre(), marcador)
                            || contieneMarcador(usuario.getEmail(), marcador))
                    .map(Usuario::getId)
                    .filter(id -> !usuarioIds.contains(id))
                    .forEach(usuarioIds::add);
        }
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para PublicadorFlowIT."));

        return ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(ciudad -> new Referencias(
                        deporte,
                        ciudad,
                        barrioRepository.findByActivoTrueAndCiudad_IdOrderByNombreAsc(ciudad.getId()).stream()
                                .findFirst()
                                .orElse(null)
                ))
                .filter(referencias -> referencias.barrio() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una ciudad activa con barrio activo para PublicadorFlowIT."
                ));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("publicador-flow-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private boolean contenidoTieneId(JsonNode pagina, Long id) {
        JsonNode contenido = pagina.path("contenido");
        if (!contenido.isArray()) {
            return false;
        }

        for (JsonNode item : contenido) {
            if (item.path("id").asLong() == id.longValue()) {
                return true;
            }
        }

        return false;
    }

    private boolean existeResiduoConMarcador(String marcador) {
        return solicitudPublicacionRepository.findAll().stream()
                .anyMatch(solicitud -> contieneMarcador(solicitud.getNombreActividad(), marcador)
                        || contieneMarcador(solicitud.getNombrePublicador(), marcador)
                        || contieneMarcador(solicitud.getEmail(), marcador))
                || perfilPublicadorRepository.findAll().stream()
                        .anyMatch(perfil -> contieneMarcador(perfil.getNombre(), marcador)
                                || contieneMarcador(perfil.getEmailContacto(), marcador))
                || usuarioRepository.findAll().stream()
                        .anyMatch(usuario -> contieneMarcador(usuario.getNombre(), marcador)
                                || contieneMarcador(usuario.getEmail(), marcador));
    }

    private boolean contieneMarcador(String texto, String marcador) {
        return texto != null && texto.contains(marcador);
    }

    private String marcadorUnico() {
        String marcador = "it-publicador-" + UUID.randomUUID();
        marcadores.add(marcador);
        return marcador;
    }

    private String jsonRegistroUsuario(String marcador, String email) {
        return """
                {
                  "nombre": "Usuario %s",
                  "apellido": "Flow",
                  "email": "%s",
                  "password": "%s",
                  "confirmarPassword": "%s",
                  "telefono": "+54 9 223 000-0000"
                }
                """.formatted(marcador, email, PASSWORD_TEST, PASSWORD_TEST);
    }

    private String jsonRegistroPublicador(String marcador, String email, String whatsapp, Long ciudadPrincipalId) {
        return """
                {
                  "nombre": "Publicador %s",
                  "apellido": "Flow",
                  "email": "%s",
                  "password": "%s",
                  "confirmarPassword": "%s",
                  "whatsapp": "%s",
                  "tipoPublicador": "ESCUELA_DEPORTIVA",
                  "nombrePublico": "Escuela %s",
                  "ciudadPrincipalId": %d,
                  "descripcion": "Perfil creado por integration-local.",
                  "instagram": "@publicador_flow_it",
                  "emailContacto": "%s",
                  "telefonoContacto": "+54 9 223 000-0002"
                }
                """.formatted(marcador, email, PASSWORD_TEST, PASSWORD_TEST, whatsapp, marcador, ciudadPrincipalId, email);
    }

    private String jsonSolicitudPublicador(Referencias referencias, String marcador, String emailContacto) {
        return """
                {
                  "nombreActividad": "Actividad %s",
                  "deporteId": %d,
                  "descripcion": "Solicitud de publicador creada por integration-local.",
                  "nivel": "PRINCIPIANTE",
                  "enfoque": "RECREATIVO",
                  "modalidad": "PRESENCIAL",
                  "edadMinima": 18,
                  "edadMaxima": 65,
                  "precioReferencia": 15000.00,
                  "mostrarPrecio": true,
                  "ciudadId": %d,
                  "barrioId": %d,
                  "nombreLugar": "Sede %s",
                  "direccion": "Calle test %s",
                  "referenciaUbicacion": "Referencia %s",
                  "whatsapp": "+54 9 223 000-0003",
                  "instagram": "@solicitud_flow_it",
                  "email": "%s",
                  "observacionesSolicitante": "Observacion %s",
                  "aceptaCondiciones": true,
                  "horarios": [
                    {
                      "diaSemana": "LUNES",
                      "horaInicio": "09:00",
                      "horaFin": "10:00",
                      "observacion": "Primer turno"
                    },
                    {
                      "diaSemana": "MIERCOLES",
                      "horaInicio": "18:00",
                      "horaFin": "19:00",
                      "observacion": "Segundo turno"
                    }
                  ]
                }
                """.formatted(
                marcador,
                referencias.deporte().getId(),
                referencias.ciudad().getId(),
                referencias.barrio().getId(),
                marcador,
                marcador,
                marcador,
                emailContacto,
                marcador
        );
    }

    private String jsonSolicitudPublica(Referencias referencias, String marcador) {
        return """
                {
                  "tipoPublicador": "ESCUELA_DEPORTIVA",
                  "nombrePublicador": "Publicador anonimo %s",
                  "nombreActividad": "Actividad anonima %s",
                  "deporteId": %d,
                  "descripcion": "Solicitud publica anonima creada por integration-local.",
                  "nivel": "TODOS",
                  "enfoque": "MIXTO",
                  "modalidad": "PRESENCIAL",
                  "edadMinima": 12,
                  "edadMaxima": 70,
                  "precioReferencia": 10000.00,
                  "mostrarPrecio": false,
                  "ciudadId": %d,
                  "barrioId": %d,
                  "nombreLugar": "Lugar anonimo %s",
                  "direccion": "Calle anonima %s",
                  "referenciaUbicacion": "Referencia anonima %s",
                  "whatsapp": "+54 9 223 000-0004",
                  "instagram": "@anonimo_flow_it",
                  "email": "anonimo-%s@dondeentreno.test",
                  "observacionesSolicitante": "Observacion anonima %s",
                  "aceptaCondiciones": true,
                  "horarios": [
                    {
                      "diaSemana": "MARTES",
                      "horaInicio": "10:00",
                      "horaFin": "11:00",
                      "observacion": "Turno anonimo"
                    }
                  ]
                }
                """.formatted(
                marcador,
                marcador,
                referencias.deporte().getId(),
                referencias.ciudad().getId(),
                referencias.barrio().getId(),
                marcador,
                marcador,
                marcador,
                marcador,
                marcador
        );
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

    private record Publicador(Usuario usuario, PerfilPublicador perfil, String email, String whatsapp) {
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }
}
