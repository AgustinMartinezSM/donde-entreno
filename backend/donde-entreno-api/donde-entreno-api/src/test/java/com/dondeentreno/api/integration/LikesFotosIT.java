package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.MeGustaImagenRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de likes en fotos (script 23, bloque 14): dar like a
 * una foto aprobada, ver el contador en el listado publico de imagenes
 * de la actividad, idempotencia, quitar, foto no visible → 404, y
 * anonimo → 401.
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
class LikesFotosIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String BASE_LIKES = "/api/usuario/likes-fotos";
    private static final String URL_PUBLICA_BASE =
            "https://proyecto.supabase.co/storage/v1/object/public/imagenes-publicas/likes-it-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private MeGustaImagenRepository meGustaImagenRepository;

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
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    private final List<Long> imagenIds = new ArrayList<>();
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
        for (Long usuarioId : usuarioIds) {
            meGustaImagenRepository.deleteAll(
                    meGustaImagenRepository.findAll().stream()
                            .filter(like -> like.getUsuarioId().equals(usuarioId))
                            .toList()
            );
        }
        meGustaImagenRepository.flush();

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

        imagenIds.clear();
        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }

    @Test
    void darLikeSubeElContadorPublicoYQuitarLoBaja() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Imagen foto = crearImagenAprobada(actividad);

        /* Like: 204, e idempotente. */
        mockMvc.perform(put(BASE_LIKES + "/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());
        mockMvc.perform(put(BASE_LIKES + "/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        /* El contador publico viaja en el listado de imagenes (sin sesion). */
        mockMvc.perform(get("/api/actividades/{slug}/imagenes", actividad.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadLikes").value(1));

        /* Los ids propios pintan el corazon. */
        mockMvc.perform(get(BASE_LIKES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(foto.getId()));

        /* Quitar: 204 idempotente, y el contador vuelve a 0. */
        mockMvc.perform(delete(BASE_LIKES + "/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(BASE_LIKES + "/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/actividades/{slug}/imagenes", actividad.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadLikes").value(0));
    }

    @Test
    void unaFotoNoVisibleDa404() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        Imagen pendiente = crearImagen(actividad, "PENDIENTE", true);
        mockMvc.perform(put(BASE_LIKES + "/{id}", pendiente.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(BASE_LIKES + "/{id}", 99_999_999L)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonimoDevuelve401EnTodo() throws Exception {
        mockMvc.perform(get(BASE_LIKES)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE_LIKES + "/{id}", 1L)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE_LIKES + "/{id}", 1L)).andExpect(status().isUnauthorized());
    }

    /* ================= helpers (mismo patron que SyncFavoritosIT) ================= */

    private Imagen crearImagenAprobada(Actividad actividad) {
        return crearImagen(actividad, "APROBADA", true);
    }

    private Imagen crearImagen(Actividad actividad, String estadoModeracion, boolean activa) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl(URL_PUBLICA_BASE + UUID.randomUUID() + ".jpg");
        imagen.setTipoImagen("GALERIA");
        imagen.setOrden(1);
        imagen.setActiva(activa);
        imagen.setEstadoModeracion(estadoModeracion);
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.saveAndFlush(imagen);
        imagenIds.add(guardada.getId());
        return guardada;
    }

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Likes IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("likes-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-likes-it");
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
        perfil.setNombre("Perfil Likes IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0801");
        perfil.setWhatsappNormalizado("5492235550801");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para LikesFotosIT."));

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
                        "No hay una ciudad activa con barrio activo para LikesFotosIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Likes IT");
        ubicacion.setDireccion("Calle likes 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Likes IT");
        actividad.setSlug(slugUnico("actividad-likes-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por LikesFotosIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0802");
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
                        .subject("likes-it@dondeentreno.test")
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
