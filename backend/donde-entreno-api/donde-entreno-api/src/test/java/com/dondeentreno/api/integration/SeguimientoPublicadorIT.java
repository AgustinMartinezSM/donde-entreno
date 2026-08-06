package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT de "seguir publicadores" (capa social, Bloque 8).
 *
 * Flujo end-to-end de un usuario que sigue/deja de seguir a un
 * publicador, idempotencia, listado, y protección (401/404). Perfil
 * integration-local, mismas guardas de host local y limpieza por
 * marcador que el resto de IT. Requiere la tabla seguimiento_publicador
 * (migración 17).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-seguimiento-publicador-it-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class SeguimientoPublicadorIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String BASE = "/api/usuario/seguimientos/publicadores";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private ActividadRepository actividadRepository;

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

        for (Long usuarioId : usuarioIds) {
            List<SeguimientoPublicador> seguimientos =
                    seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(usuarioId);
            seguimientoPublicadorRepository.deleteAll(seguimientos);
        }
        seguimientoPublicadorRepository.flush();

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

    @Test
    void flujoSeguirEstadoListaYDejarDeSeguir() throws Exception {
        String marcador = marcadorUnico();
        Ciudad ciudad = obtenerCiudadActiva();
        Usuario seguidor = crearUsuario(marcador, ROL_USUARIO);
        PerfilPublicador objetivo = crearPerfilObjetivo(marcador, ciudad);

        // Antes de seguir: estado false.
        mockMvc.perform(get(BASE + "/{id}/estado", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siguiendo").value(false));

        // Seguir: 200 y siguiendo true.
        mockMvc.perform(post(BASE + "/{id}", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siguiendo").value(true));

        // Seguir de nuevo: idempotente.
        mockMvc.perform(post(BASE + "/{id}", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siguiendo").value(true));

        // Estado ahora true.
        mockMvc.perform(get(BASE + "/{id}/estado", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siguiendo").value(true));

        // Lista: exactamente 1, el publicador objetivo.
        mockMvc.perform(get(BASE)
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].perfilPublicadorId").value(objetivo.getId()))
                .andExpect(jsonPath("$[0].perfilPublicadorNombre").value(objetivo.getNombre()));

        // Dejar de seguir: 204.
        mockMvc.perform(delete(BASE + "/{id}", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isNoContent());

        // Estado vuelve a false y lista vacía.
        mockMvc.perform(get(BASE + "/{id}/estado", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siguiendo").value(false));
        mockMvc.perform(get(BASE)
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void feedDevuelveActividadesDeLosPublicadoresSeguidos() throws Exception {
        String marcador = marcadorUnico();
        Referencias referencias = obtenerReferenciasActivas();
        Usuario seguidor = crearUsuario(marcador, ROL_USUARIO);
        PerfilPublicador objetivo = crearPerfilObjetivo(marcadorUnico(), referencias.ciudad());
        Actividad actividad = crearActividadPublicada(marcadorUnico(), objetivo, referencias);

        // Sin seguir a nadie, el feed viene vacío.
        mockMvc.perform(get("/api/usuario/feed/actividades")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post(BASE + "/{id}", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk());

        // Tras seguir, el feed trae la actividad del publicador seguido,
        // con los campos que la UI muestra (título, publicador, deporte,
        // ciudad y slug para armar el link al detalle).
        mockMvc.perform(get("/api/usuario/feed/actividades")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(actividad.getId()))
                .andExpect(jsonPath("$[0].titulo").value(actividad.getTitulo()))
                .andExpect(jsonPath("$[0].slug").value(actividad.getSlug()))
                .andExpect(jsonPath("$[0].perfilPublicadorNombre").value(objetivo.getNombre()))
                .andExpect(jsonPath("$[0].deporteNombre").value(referencias.deporte().getNombre()))
                .andExpect(jsonPath("$[0].ciudadNombre").value(referencias.ciudad().getNombre()));
    }

    @Test
    void feedExcluyeActividadesNoVisiblesDelPublicadorSeguido() throws Exception {
        String marcador = marcadorUnico();
        Referencias referencias = obtenerReferenciasActivas();
        Usuario seguidor = crearUsuario(marcador, ROL_USUARIO);
        PerfilPublicador objetivo = crearPerfilObjetivo(marcadorUnico(), referencias.ciudad());

        // Del mismo publicador seguido: una visible y tres que NO deben
        // aparecer en el feed (no publicada, inactiva, borrada).
        Actividad visible = crearActividadPublicada(marcadorUnico(), objetivo, referencias);
        crearActividad(marcadorUnico(), objetivo, referencias, "BORRADOR", true, null);
        crearActividad(marcadorUnico(), objetivo, referencias, "PUBLICADA", false, null);
        crearActividad(marcadorUnico(), objetivo, referencias, "PUBLICADA", true, OffsetDateTime.now());

        mockMvc.perform(post(BASE + "/{id}", objetivo.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuario/feed/actividades")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visible.getId()));
    }

    @Test
    void feedSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuario/feed/actividades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void seguirSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(post(BASE + "/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void seguirAPublicadorInexistenteDevuelve404() throws Exception {
        String marcador = marcadorUnico();
        Usuario seguidor = crearUsuario(marcador, ROL_USUARIO);

        mockMvc.perform(post(BASE + "/{id}", Long.MAX_VALUE - 5)
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private Usuario crearUsuario(String marcador, String rolNombre) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario " + marcador);
        usuario.setApellido("Seguimiento IT");
        usuario.setEmail("seguimiento-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-seguimiento-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private PerfilPublicador crearPerfilObjetivo(String marcador, Ciudad ciudad) {
        verificarDatasourceLocal();

        Usuario duenio = crearUsuario(marcador + "-DUENIO", ROL_PUBLICADOR);
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil objetivo " + marcador);
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0401");
        perfil.setWhatsappNormalizado("5492235550401");
        perfil.setTelefonoContacto("+54 9 223 555-0402");
        perfil.setTelefonoContactoNormalizado("5492235550402");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador guardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(guardado.getId());
        return guardado;
    }

    private Ciudad obtenerCiudadActiva() {
        return ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay ciudades activas para SeguimientoPublicadorIT."));
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para SeguimientoPublicadorIT."));

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
                        "No hay una ciudad activa con barrio activo para SeguimientoPublicadorIT."
                ));
    }

    private Actividad crearActividadPublicada(
            String marcador,
            PerfilPublicador perfil,
            Referencias referencias
    ) {
        return crearActividad(marcador, perfil, referencias, "PUBLICADA", true, null);
    }

    private Actividad crearActividad(
            String marcador,
            PerfilPublicador perfil,
            Referencias referencias,
            String estadoPublicacion,
            boolean activa,
            OffsetDateTime deletedAt
    ) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede " + marcador);
        ubicacion.setDireccion("Calle seguimiento " + marcador);
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
        actividad.setTitulo("Actividad " + marcador);
        actividad.setSlug(slugUnico("actividad-seguimiento-" + marcador));
        actividad.setDescripcion("Actividad creada por SeguimientoPublicadorIT " + marcador);
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0403");
        actividad.setInstagramContacto("@seguimiento_it");
        actividad.setEmailContacto(perfil.getEmailContacto());
        actividad.setEstadoPublicacion(estadoPublicacion);
        actividad.setActiva(activa);
        actividad.setDeletedAt(deletedAt);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad actividadGuardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(actividadGuardada.getId());
        return actividadGuardada;
    }

    private String slugUnico(String base) {
        return base.toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("seguimiento-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private boolean existeResiduoConMarcador(String marcador) {
        return perfilPublicadorRepository.findAll().stream()
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
        String marcador = "IT-SEGUIMIENTO-" + UUID.randomUUID();
        marcadores.add(marcador);
        return marcador;
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
}
