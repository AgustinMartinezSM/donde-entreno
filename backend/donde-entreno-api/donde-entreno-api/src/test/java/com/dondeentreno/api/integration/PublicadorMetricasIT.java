package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT del endpoint de métricas del publicador (GET /api/publicador/metricas).
 *
 * Crea un publicador con datos conocidos (2 actividades publicadas + 1
 * imagen PENDIENTE) y verifica que los conteos sean correctos y acotados
 * al perfil autenticado, además de la protección por rol (401/403).
 *
 * Perfil integration-local, mismas guardas de host local y limpieza por
 * marcador que el resto de IT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-publicador-metricas-it-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class PublicadorMetricasIT {

    private static final String ESTADO_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ESTADO_MODERACION_PENDIENTE = "PENDIENTE";
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
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    @Autowired
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    private final List<Long> seguimientoIds = new ArrayList<>();
    private final List<Long> imagenIds = new ArrayList<>();
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

        for (Long seguimientoId : seguimientoIds) {
            seguimientoPublicadorRepository.findById(seguimientoId)
                    .ifPresent(seguimientoPublicadorRepository::delete);
        }
        seguimientoPublicadorRepository.flush();

        for (Long imagenId : imagenIds) {
            imagenRepository.findById(imagenId).ifPresent(imagenRepository::delete);
        }
        imagenRepository.flush();

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

        seguimientoIds.clear();
        imagenIds.clear();
        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
        marcadores.clear();
    }

    @Test
    void metricasReflejanLosDatosDelPublicadorAutenticado() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Actividad actividadA = crearActividadPublicada(marcadorUnico(), publicador.perfil(), referencias);
        crearActividadPublicada(marcadorUnico(), publicador.perfil(), referencias);
        crearImagenPendiente(actividadA);
        Usuario seguidor = crearSeguidor(marcadorUnico());
        crearSeguimiento(seguidor, publicador.perfil());

        mockMvc.perform(get("/api/publicador/metricas")
                        .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadesPublicadas").value(2))
                .andExpect(jsonPath("$.solicitudesPublicacionPendientes").value(0))
                .andExpect(jsonPath("$.solicitudesCambioPendientes").value(0))
                .andExpect(jsonPath("$.imagenesPendientesModeracion").value(1))
                .andExpect(jsonPath("$.seguidores").value(1));
    }

    @Test
    void metricasDeUnPublicadorNuevoSonTodasCero() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        Publicador publicador = crearPublicador(marcadorUnico(), referencias.ciudad());

        mockMvc.perform(get("/api/publicador/metricas")
                        .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadesPublicadas").value(0))
                .andExpect(jsonPath("$.solicitudesPublicacionPendientes").value(0))
                .andExpect(jsonPath("$.solicitudesCambioPendientes").value(0))
                .andExpect(jsonPath("$.imagenesPendientesModeracion").value(0))
                .andExpect(jsonPath("$.seguidores").value(0));
    }

    @Test
    void metricasRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/publicador/metricas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void metricasRequierenRolPublicador() throws Exception {
        mockMvc.perform(get("/api/publicador/metricas")
                        .with(jwtConRol(ROL_USUARIO, Long.MAX_VALUE - 10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private Publicador crearPublicador(String marcador, Ciudad ciudad) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_PUBLICADOR)
                .orElseThrow(() -> new IllegalStateException("No existe el rol PUBLICADOR para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Publicador " + marcador);
        usuario.setApellido("Metricas IT");
        usuario.setEmail("publicador-metricas-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-publicador-metricas-it");
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
        perfil.setWhatsapp("+54 9 223 555-0301");
        perfil.setWhatsappNormalizado("5492235550301");
        perfil.setTelefonoContacto("+54 9 223 555-0302");
        perfil.setTelefonoContactoNormalizado("5492235550302");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador perfilGuardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(perfilGuardado.getId());

        return new Publicador(usuarioGuardado, perfilGuardado);
    }

    private Actividad crearActividadPublicada(
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
        ubicacion.setDireccion("Calle metricas " + marcador);
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
        actividad.setSlug(slugUnico("actividad-metricas-" + marcador));
        actividad.setDescripcion("Actividad creada por PublicadorMetricasIT " + marcador);
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0303");
        actividad.setInstagramContacto("@publicador_metricas_it");
        actividad.setEmailContacto(perfil.getEmailContacto());
        actividad.setEstadoPublicacion(ESTADO_PUBLICADA);
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad actividadGuardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(actividadGuardada.getId());
        return actividadGuardada;
    }

    private Usuario crearSeguidor(String marcador) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_USUARIO)
                .orElseThrow(() -> new IllegalStateException("No existe el rol USUARIO para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Seguidor " + marcador);
        usuario.setApellido("Metricas IT");
        usuario.setEmail("seguidor-metricas-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-seguidor-metricas-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private void crearSeguimiento(Usuario seguidor, PerfilPublicador perfil) {
        verificarDatasourceLocal();

        SeguimientoPublicador seguimiento = new SeguimientoPublicador();
        seguimiento.setUsuario(seguidor);
        seguimiento.setPerfilPublicador(perfil);
        seguimiento.setCreatedAt(OffsetDateTime.now());

        SeguimientoPublicador guardado = seguimientoPublicadorRepository.saveAndFlush(seguimiento);
        seguimientoIds.add(guardado.getId());
    }

    private void crearImagenPendiente(Actividad actividad) {
        verificarDatasourceLocal();

        // La imagen cuelga solo de la actividad (dueño único: la
        // constraint chk_imagen_duenio_unico prohíbe perfil + actividad).
        OffsetDateTime ahora = OffsetDateTime.now();
        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl("/recursos/metricas-it/" + UUID.randomUUID() + ".jpg");
        imagen.setTipoImagen("GALERIA");
        imagen.setOrden(0);
        imagen.setActiva(false);
        imagen.setEstadoModeracion(ESTADO_MODERACION_PENDIENTE);
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        Imagen imagenGuardada = imagenRepository.saveAndFlush(imagen);
        imagenIds.add(imagenGuardada.getId());
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para PublicadorMetricasIT."));

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
                        "No hay una ciudad activa con barrio activo para PublicadorMetricasIT."
                ));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("publicador-metricas-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
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
        String marcador = "IT-PUBLICADOR-METRICAS-" + UUID.randomUUID();
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
