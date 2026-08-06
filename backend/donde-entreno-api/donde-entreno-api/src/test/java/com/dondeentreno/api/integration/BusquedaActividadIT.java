package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Red de seguridad de la busqueda publica insensible a tildes (Bloque 9,
 * etapa backend). Es la unica prueba que ejercita el SQL real de la
 * busqueda por texto: los tests unitarios de service/controller mockean
 * el repository y no disparan la funcion unaccent.
 *
 * Cubre las DOS queries con rama de texto de ActividadRepository:
 * - buscarActividadesPublicadasConFiltrosPaginado (la que usa el endpoint
 *   publico GET /api/actividades): se ejercita via HTTP.
 * - buscarActividadesPublicadasConFiltros (no paginada, sin endpoint HTTP
 *   que la invoque hoy): se ejercita con una llamada directa al repository.
 *
 * Requiere PostgreSQL local con la extension unaccent
 * (migracion database/scripts/16_prepare_busqueda_unaccent.sql).
 * Mismas guardas de host local y limpieza por marcador que el resto de IT.
 *
 * Ver docs/plan-busqueda-sin-tildes.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-busqueda-actividad-it-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class BusquedaActividadIT {

    private static final String ESTADO_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";

    /** Palabra con tilde que se guarda en el titulo de la actividad de prueba. */
    private static final String PALABRA_CON_TILDE = "Natación";
    /** Misma palabra escrita sin tilde: solo matchea via unaccent. */
    private static final String BUSQUEDA_SIN_TILDE = "natacion";
    /** La misma palabra con tilde: debe seguir encontrando la actividad. */
    private static final String BUSQUEDA_CON_TILDE = "natación";
    /** Texto que no aparece en ningun campo de la actividad de prueba. */
    private static final String BUSQUEDA_SIN_MATCH = "terminoinexistentebusquedait";
    /** Texto con un comodin LIKE (%) que debe tratarse como literal. */
    private static final String BUSQUEDA_COMODIN = "50%";

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
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    private final List<Long> actividadIds = new ArrayList<>();
    private final List<Long> ubicacionIds = new ArrayList<>();
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

        for (Long actividadId : actividadIds) {
            actividadRepository.findById(actividadId).ifPresent(actividadRepository::delete);
        }
        actividadRepository.flush();

        for (Long ubicacionId : ubicacionIds) {
            ubicacionRepository.findById(ubicacionId).ifPresent(ubicacionRepository::delete);
        }
        ubicacionRepository.flush();

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

        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
        marcadores.clear();
    }

    /**
     * Query paginada (la del endpoint publico GET /api/actividades):
     * buscar sin tilde encuentra la actividad guardada con tilde; buscar
     * con tilde tambien la encuentra; un texto que no matchea no la trae.
     */
    @Test
    void busquedaPublicaPaginadaEsInsensibleATildes() throws Exception {
        String marcador = marcadorUnico();
        Referencias referencias = obtenerReferenciasActivas();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Actividad actividad = crearActividadConTitulo(
                PALABRA_CON_TILDE + " " + marcador, marcador, publicador.perfil(), referencias);

        JsonNode sinTilde = buscarPublico(BUSQUEDA_SIN_TILDE);
        assertTrue(contenidoTieneId(sinTilde, actividad.getId()),
                "Buscar '" + BUSQUEDA_SIN_TILDE + "' (sin tilde) debe encontrar la actividad con titulo '"
                        + PALABRA_CON_TILDE + "' via unaccent.");

        JsonNode conTilde = buscarPublico(BUSQUEDA_CON_TILDE);
        assertTrue(contenidoTieneId(conTilde, actividad.getId()),
                "Buscar '" + BUSQUEDA_CON_TILDE + "' (con tilde) debe seguir encontrando la actividad.");

        JsonNode sinMatch = buscarPublico(BUSQUEDA_SIN_MATCH);
        assertFalse(contenidoTieneId(sinMatch, actividad.getId()),
                "Un texto que no aparece en la actividad no debe traerla (la busqueda no matchea todo).");
    }

    /**
     * Query no paginada buscarActividadesPublicadasConFiltros: hoy no la
     * invoca ningun endpoint HTTP, asi que se ejercita directo por el
     * repository para verificar que tambien es insensible a tildes.
     */
    @Test
    void busquedaNoPaginadaEsInsensibleATildes() {
        String marcador = marcadorUnico();
        Referencias referencias = obtenerReferenciasActivas();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Actividad actividad = crearActividadConTitulo(
                PALABRA_CON_TILDE + " " + marcador, marcador, publicador.perfil(), referencias);

        List<Actividad> sinTilde = buscarPorTextoNoPaginada(BUSQUEDA_SIN_TILDE);
        assertTrue(listaTieneId(sinTilde, actividad.getId()),
                "La query no paginada debe encontrar la actividad con tilde buscando sin tilde.");

        List<Actividad> sinMatch = buscarPorTextoNoPaginada(BUSQUEDA_SIN_MATCH);
        assertFalse(listaTieneId(sinMatch, actividad.getId()),
                "La query no paginada no debe traer la actividad con un texto que no matchea.");
    }

    /**
     * Los comodines de LIKE en el texto del usuario se buscan literal:
     * "50%" encuentra un título con "50%" pero no uno con "50 ..." (si el
     * % actuara como comodín, traería ambos).
     */
    @Test
    void busquedaTrataLosComodinesLikeComoLiterales() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcadorConSimbolo = marcadorUnico();
        String marcadorSinSimbolo = marcadorUnico();
        Publicador publicador = crearPublicador(marcadorUnico(), referencias.ciudad());

        Actividad conSimbolo = crearActividadConTitulo(
                "Descuento 50% " + marcadorConSimbolo, marcadorConSimbolo, publicador.perfil(), referencias);
        Actividad sinSimbolo = crearActividadConTitulo(
                "50 por ciento " + marcadorSinSimbolo, marcadorSinSimbolo, publicador.perfil(), referencias);

        JsonNode resultado = buscarPublico(BUSQUEDA_COMODIN);

        assertTrue(contenidoTieneId(resultado, conSimbolo.getId()),
                "Buscar '" + BUSQUEDA_COMODIN + "' debe encontrar el título que contiene '50%' literal.");
        assertFalse(contenidoTieneId(resultado, sinSimbolo.getId()),
                "Buscar '" + BUSQUEDA_COMODIN + "' NO debe traer un título con '50 ...' (el % no es comodín).");
    }

    private JsonNode buscarPublico(String texto) throws Exception {
        String response = mockMvc.perform(get("/api/actividades")
                        .param("texto", texto)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private List<Actividad> buscarPorTextoNoPaginada(String texto) {
        return actividadRepository.buscarActividadesPublicadasConFiltros(
                ESTADO_PUBLICADA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                texto
        );
    }

    private Publicador crearPublicador(String marcador, Ciudad ciudad) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_PUBLICADOR)
                .orElseThrow(() -> new IllegalStateException("No existe el rol PUBLICADOR para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Publicador " + marcador);
        usuario.setApellido("Busqueda IT");
        usuario.setEmail("busqueda-actividad-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-busqueda-actividad-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(usuarioGuardado.getId());

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(usuarioGuardado);
        perfil.setNombre("Perfil " + marcador);
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(usuarioGuardado.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0201");
        perfil.setWhatsappNormalizado("5492235550201");
        perfil.setTelefonoContacto("+54 9 223 555-0202");
        perfil.setTelefonoContactoNormalizado("5492235550202");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador perfilGuardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(perfilGuardado.getId());

        return new Publicador(usuarioGuardado, perfilGuardado);
    }

    private Actividad crearActividadConTitulo(
            String titulo,
            String marcador,
            PerfilPublicador perfil,
            Referencias referencias
    ) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede " + marcador);
        ubicacion.setDireccion("Calle busqueda " + marcador);
        ubicacion.setReferencia("Referencia " + marcador);
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo(titulo);
        actividad.setSlug(slugUnico("actividad-busqueda-" + marcador));
        actividad.setDescripcion("Actividad creada por BusquedaActividadIT " + marcador);
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0203");
        actividad.setInstagramContacto("@busqueda_actividad_it");
        actividad.setEmailContacto(perfil.getEmailContacto());
        actividad.setEstadoPublicacion(ESTADO_PUBLICADA);
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad actividadGuardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(actividadGuardada.getId());
        return actividadGuardada;
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para BusquedaActividadIT."));

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
                        "No hay una ciudad activa con barrio activo para BusquedaActividadIT."
                ));
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

    private boolean listaTieneId(List<Actividad> actividades, Long id) {
        return actividades.stream().anyMatch(actividad -> id.equals(actividad.getId()));
    }

    private void registrarResiduosPorMarcador() {
        for (String marcador : marcadores) {
            actividadRepository.findAll().stream()
                    .filter(actividad -> contieneMarcador(actividad.getTitulo(), marcador)
                            || contieneMarcador(actividad.getSlug(), marcador))
                    .map(Actividad::getId)
                    .filter(id -> !actividadIds.contains(id))
                    .forEach(actividadIds::add);

            ubicacionRepository.findAll().stream()
                    .filter(ubicacion -> contieneMarcador(ubicacion.getNombre(), marcador)
                            || contieneMarcador(ubicacion.getDireccion(), marcador)
                            || contieneMarcador(ubicacion.getReferencia(), marcador))
                    .map(Ubicacion::getId)
                    .filter(id -> !ubicacionIds.contains(id))
                    .forEach(ubicacionIds::add);

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

    private boolean existeResiduoConMarcador(String marcador) {
        return actividadRepository.findAll().stream()
                .anyMatch(actividad -> contieneMarcador(actividad.getTitulo(), marcador)
                        || contieneMarcador(actividad.getSlug(), marcador))
                || ubicacionRepository.findAll().stream()
                        .anyMatch(ubicacion -> contieneMarcador(ubicacion.getNombre(), marcador)
                                || contieneMarcador(ubicacion.getDireccion(), marcador)
                                || contieneMarcador(ubicacion.getReferencia(), marcador))
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
        String marcador = "IT-BUSQUEDA-ACTIVIDAD-" + UUID.randomUUID();
        marcadores.add(marcador);
        return marcador;
    }

    private String slugUnico(String base) {
        return base.toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
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

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        return url.toLowerCase(Locale.ROOT).matches(
                ".*(supabase|render|amazonaws|azure|neon|railway|aiven|digitalocean|\\.com|\\.net|\\.io|\\.app).*"
        );
    }

    private record Publicador(Usuario usuario, PerfilPublicador perfil) {
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }
}
