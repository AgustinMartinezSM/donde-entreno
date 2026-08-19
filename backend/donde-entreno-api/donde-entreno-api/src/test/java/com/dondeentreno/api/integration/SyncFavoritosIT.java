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
import com.dondeentreno.api.repository.DeportePreferidoRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT del sync de favoritos y deportes preferidos (script 20).
 *
 * Flujo end-to-end por HTTP: guardar/quitar por slug con idempotencia,
 * listado como cards publicas (una despublicada desaparece sin borrar la
 * fila), reemplazo del conjunto de deportes con filtro de catalogo, y la
 * regla explicita /api/usuario/** (anonimo → 401).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-sync-favoritos-it-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class SyncFavoritosIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String BASE_FAVORITOS = "/api/usuario/favoritos";
    private static final String BASE_DEPORTES = "/api/usuario/deportes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private FavoritoActividadRepository favoritoActividadRepository;

    @Autowired
    private DeportePreferidoRepository deportePreferidoRepository;

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
        verificarVariablesLocalesPresentes();

        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    /*
      Favoritos y deportes caen solos por el ON DELETE CASCADE al borrar
      usuarios; se borran explicito igual para que la limpieza no dependa
      de la base (y delate un script 20 sin cascade).
    */
    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        for (Long usuarioId : usuarioIds) {
            favoritoActividadRepository.deleteAll(
                    favoritoActividadRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)
            );
        }
        favoritoActividadRepository.flush();

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

    @Test
    void guardarListarYQuitarPorSlugConIdempotencia() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        /* Guardar: 204. Repetir: 204 y sigue habiendo UNA fila. */
        mockMvc.perform(put(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());
        mockMvc.perform(put(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        assertEquals(1, favoritoActividadRepository
                .findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).size());

        /* El listado devuelve la card publica completa. */
        mockMvc.perform(get(BASE_FAVORITOS)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value(actividad.getSlug()))
                .andExpect(jsonPath("$[0].titulo").value(actividad.getTitulo()))
                .andExpect(jsonPath("$[0].deporteNombre").value(referencias.deporte().getNombre()));

        /* Quitar: 204. Repetir: 204 igual (idempotente, sin delatar nada). */
        mockMvc.perform(delete(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_FAVORITOS)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unaActividadDespublicadaDesapareceDelListadoSinPerderElFavorito() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        mockMvc.perform(put(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        actividad.setActiva(false);
        actividadRepository.saveAndFlush(actividad);

        mockMvc.perform(get(BASE_FAVORITOS)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        /* La fila queda: si la actividad se republica, el favorito vuelve. */
        assertEquals(1, favoritoActividadRepository
                .findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).size());
    }

    @Test
    void guardarUnSlugInexistenteDevuelve404() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);

        mockMvc.perform(put(BASE_FAVORITOS + "/{slug}", "no-existe-para-nada")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void losDeportesSeReemplazanComoConjuntoFiltradoContraElCatalogo() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        List<Deporte> catalogo = deporteRepository.findByActivoTrue();
        assertTrue(catalogo.size() >= 2, "El seed local necesita al menos dos deportes activos.");
        String primero = catalogo.get(0).getSlug();
        String segundo = catalogo.get(1).getSlug();

        /* El PUT devuelve lo efectivamente guardado, ya filtrado y en orden. */
        mockMvc.perform(put(BASE_DEPORTES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slugs": ["%s", "slug-inventado", "%s", "%s"]}
                                """.formatted(segundo, primero, segundo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value(segundo))
                .andExpect(jsonPath("$[1]").value(primero));

        mockMvc.perform(get(BASE_DEPORTES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value(segundo));

        /* Reemplazo con menos: el conjunto viejo no sobrevive. */
        mockMvc.perform(put(BASE_DEPORTES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slugs": ["%s"]}
                                """.formatted(primero)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        assertEquals(List.of(primero), deportePreferidoRepository.slugsDe(usuario.getId()));
    }

    @Test
    void sinAutenticacionTodoDevuelve401() throws Exception {
        mockMvc.perform(get(BASE_FAVORITOS)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE_FAVORITOS + "/{slug}", "algo")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_DEPORTES)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE_DEPORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slugs\": []}"))
                .andExpect(status().isUnauthorized());
    }

    /* ================= helpers (mismo patron que SeguimientoPublicadorIT) ================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Sync IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("sync-favoritos-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-sync-it");
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
        perfil.setNombre("Perfil Sync IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0501");
        perfil.setWhatsappNormalizado("5492235550501");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para SyncFavoritosIT."));

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
                        "No hay una ciudad activa con barrio activo para SyncFavoritosIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Sync IT");
        ubicacion.setDireccion("Calle sync 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Sync IT");
        actividad.setSlug(slugUnico("actividad-sync-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por SyncFavoritosIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0502");
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
                        .subject("sync-favoritos-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
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
        String urlNormalizada = url.toLowerCase();
        return urlNormalizada.contains("supabase") || urlNormalizada.contains("render") || urlNormalizada.contains("pooler");
    }
}
