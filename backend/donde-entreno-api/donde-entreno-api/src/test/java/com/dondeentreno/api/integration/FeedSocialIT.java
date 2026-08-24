package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.FeedEvent;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.FeedEventRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feed de eventos paginado (script 32, Fase 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
/* Mismos valores que RefreshTokenIT a proposito: contexto compartido. */
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-integration-local-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class FeedSocialIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private FeedEventRepository feedEventRepository;

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

    @BeforeEach
    void verificarDatasourceLocal() {
        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        for (Long perfilId : perfilPublicadorIds) {
            feedEventRepository.deleteAll(
                    feedEventRepository.findAll().stream()
                            .filter(evento -> perfilId.equals(evento.getPerfilPublicadorId()))
                            .toList());
        }
        feedEventRepository.flush();

        for (Long usuarioId : usuarioIds) {
            seguimientoPublicadorRepository.deleteAll(
                    seguimientoPublicadorRepository.findAll().stream()
                            .filter(seguimiento -> seguimiento.getUsuario() != null
                                    && usuarioId.equals(seguimiento.getUsuario().getId()))
                            .toList());
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

        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }

    /**
     * El corazón de la fase: el feed pagina de verdad. La V1 cortaba
     * en 20 sin forma de pedir la página siguiente.
     */
    @Test
    void elFeedPaginaYRespetaElOrdenCronologico() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, perfil);

        /* Tres hechos, del más viejo al más nuevo. */
        crearEvento(perfil, actividad, "ACTIVIDAD_NUEVA", OffsetDateTime.now().minusDays(3));
        crearEvento(perfil, actividad, "FOTOS_NUEVAS", OffsetDateTime.now().minusDays(2));
        crearEvento(perfil, actividad, "ACTIVIDAD_ACTUALIZADA", OffsetDateTime.now().minusDays(1));

        /* Página 0 de tamaño 2: los DOS más nuevos, y no es la última. */
        mockMvc.perform(get("/api/usuario/feed")
                        .param("page", "0")
                        .param("size", "2")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ACTIVIDAD_ACTUALIZADA"))
                .andExpect(jsonPath("$.contenido[1].tipo").value("FOTOS_NUEVAS"))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.ultima").value(false));

        /* Página 1: el que falta, y ahora sí es la última. */
        mockMvc.perform(get("/api/usuario/feed")
                        .param("page", "1")
                        .param("size", "2")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ACTIVIDAD_NUEVA"))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    /**
     * Cada evento llega listo para pintar: identidad del publicador y
     * datos de la actividad, sin que el frontend pida nada más.
     */
    @Test
    void cadaEventoTraeLaIdentidadYLaActividadResueltas() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, perfil);
        crearEvento(perfil, actividad, "ACTIVIDAD_NUEVA", OffsetDateTime.now());

        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].perfilNombre").value(perfil.getNombre()))
                .andExpect(jsonPath("$.contenido[0].perfilSlug").exists())
                .andExpect(jsonPath("$.contenido[0].actividadTitulo").value(actividad.getTitulo()))
                .andExpect(jsonPath("$.contenido[0].actividadSlug").value(actividad.getSlug()));
    }

    /** Quien no sigue a nadie recibe una página vacía, no un error. */
    @Test
    void sinSeguidosElFeedVieneVacio() throws Exception {
        Usuario solitario = crearUsuario(ROL_USUARIO);

        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, solitario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    /** Solo se ven los hechos de QUIEN se sigue. */
    @Test
    void losHechosDeOtrosPublicadoresNoAparecen() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador seguido = crearPerfilPublicador(referencias.ciudad());
        PerfilPublicador ajeno = crearPerfilPublicador(referencias.ciudad());
        Actividad delSeguido = crearActividadPublicada(seguido, referencias);
        Actividad delAjeno = crearActividadPublicada(ajeno, referencias);

        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, seguido);

        crearEvento(seguido, delSeguido, "ACTIVIDAD_NUEVA", OffsetDateTime.now());
        crearEvento(ajeno, delAjeno, "ACTIVIDAD_NUEVA", OffsetDateTime.now());

        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].perfilNombre").value(seguido.getNombre()));
    }

    @Test
    void elFeedExigeSesion() throws Exception {
        mockMvc.perform(get("/api/usuario/feed"))
                .andExpect(status().isUnauthorized());
    }

    /* ======================= fixtures ======================= */

    private FeedEvent crearEvento(
            PerfilPublicador perfil,
            Actividad actividad,
            String tipo,
            OffsetDateTime cuando
    ) {
        FeedEvent evento = new FeedEvent();
        evento.setTipo(tipo);
        evento.setPerfilPublicadorId(perfil.getId());
        evento.setActividadId(actividad != null ? actividad.getId() : null);
        evento.setCreatedAt(cuando);

        return feedEventRepository.saveAndFlush(evento);
    }

    private void seguir(Usuario usuario, PerfilPublicador perfil) {
        SeguimientoPublicador seguimiento = new SeguimientoPublicador();
        seguimiento.setUsuario(usuario);
        seguimiento.setPerfilPublicador(perfil);
        seguimiento.setCreatedAt(OffsetDateTime.now());
        seguimientoPublicadorRepository.saveAndFlush(seguimiento);
    }

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Feed IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("feed-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-feed-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private PerfilPublicador crearPerfilPublicador(Ciudad ciudad) {
        Usuario duenio = crearUsuario(ROL_PUBLICADOR);
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Feed IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1501");
        perfil.setWhatsappNormalizado("5492235551501");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador guardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(guardado.getId());
        return guardado;
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para FeedSocialIT."));

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
                        "No hay una ciudad activa con barrio activo para FeedSocialIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Feed IT");
        ubicacion.setDireccion("Calle feed 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Feed IT");
        actividad.setSlug(slugUnico("actividad-feed-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por FeedSocialIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-1502");
        actividad.setEmailContacto(perfil.getEmailContacto());
        actividad.setEstadoPublicacion("PUBLICADA");
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad guardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(guardada.getId());
        return guardada;
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

    private RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("feed-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        String urlNormalizada = url.toLowerCase();
        return urlNormalizada.contains("supabase") || urlNormalizada.contains("render") || urlNormalizada.contains("pooler");
    }
}
