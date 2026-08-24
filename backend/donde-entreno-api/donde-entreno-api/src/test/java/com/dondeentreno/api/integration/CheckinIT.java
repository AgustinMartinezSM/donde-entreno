package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.FavoritoActividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.EntrenamientoUsuarioRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo del check-in "Entrené acá" (script 26): registrar
 * (201), idempotencia del mismo día (200 sin fila nueva), estado del
 * botón, social proof en el detalle público (personas distintas +
 * favoritos), actividad no pública → 404 y anónimo → 401.
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
class CheckinIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String BASE_CHECKINS = "/api/usuario/checkins";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private EntrenamientoUsuarioRepository entrenamientoUsuarioRepository;

    @Autowired
    private FavoritoActividadRepository favoritoActividadRepository;

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
        for (Long usuarioId : usuarioIds) {
            entrenamientoUsuarioRepository.deleteAll(
                    entrenamientoUsuarioRepository.findAll().stream()
                            .filter(checkin -> checkin.getUsuarioId().equals(usuarioId))
                            .toList()
            );
            favoritoActividadRepository.deleteAll(
                    favoritoActividadRepository.findAll().stream()
                            .filter(favorito -> favorito.getUsuarioId().equals(usuarioId))
                            .toList()
            );
        }
        entrenamientoUsuarioRepository.flush();
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
    void registrarEsIdempotentePorDiaYAlimentaElSocialProofDelDetalle() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        /* Primer check-in del día: 201 con fila nueva. */
        mockMvc.perform(post(BASE_CHECKINS + "/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yaRegistradoHoy").value(true))
                .andExpect(jsonPath("$.registradoAhora").value(true))
                .andExpect(jsonPath("$.cantidadPersonasEntrenaron30Dias").value(1));

        /* Segundo intento el mismo día: 200 y NINGUNA fila nueva. */
        mockMvc.perform(post(BASE_CHECKINS + "/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registradoAhora").value(false))
                .andExpect(jsonPath("$.cantidadPersonasEntrenaron30Dias").value(1));

        long filas = entrenamientoUsuarioRepository.findAll().stream()
                .filter(checkin -> checkin.getUsuarioId().equals(usuario.getId()))
                .count();
        assertEquals(1L, filas);

        /* El estado del botón lo confirma sin crear nada. */
        mockMvc.perform(get(BASE_CHECKINS + "/{id}/hoy", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaRegistradoHoy").value(true))
                .andExpect(jsonPath("$.registradoAhora").value(false));

        /* Deshacer (hallazgo del smoke): la fila de hoy cae y se puede volver. */
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete(BASE_CHECKINS + "/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaRegistradoHoy").value(false))
                .andExpect(jsonPath("$.cantidadPersonasEntrenaron30Dias").value(0));

        long filasTrasQuitar = entrenamientoUsuarioRepository.findAll().stream()
                .filter(checkin -> checkin.getUsuarioId().equals(usuario.getId()))
                .count();
        assertEquals(0L, filasTrasQuitar);

        mockMvc.perform(post(BASE_CHECKINS + "/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradoAhora").value(true));

        /* Un favorito real para la segunda señal. */
        FavoritoActividad favorito = new FavoritoActividad();
        favorito.setUsuarioId(usuario.getId());
        favorito.setActividadId(actividad.getId());
        favorito.setCreatedAt(OffsetDateTime.now());
        favoritoActividadRepository.saveAndFlush(favorito);

        /* El detalle público (sin sesión) trae las señales agregadas. */
        mockMvc.perform(get("/api/actividades/{slug}/detalle", actividad.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socialProof.cantidadPersonasEntrenaron30Dias").value(1))
                .andExpect(jsonPath("$.socialProof.cantidadFavoritos").value(1))
                .andExpect(jsonPath("$.socialProof.cantidadLikesFotos").value(0));
    }

    @Test
    void actividadNoPublicaDa404() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad pausada = crearActividadPublicada(perfil, referencias);
        pausada.setEstadoPublicacion("PAUSADA");
        actividadRepository.saveAndFlush(pausada);

        mockMvc.perform(post(BASE_CHECKINS + "/{id}", pausada.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(BASE_CHECKINS + "/{id}", 99_999_999L)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonimoDevuelve401EnTodo() throws Exception {
        mockMvc.perform(post(BASE_CHECKINS + "/{id}", 1L)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_CHECKINS + "/{id}/hoy", 1L)).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete(BASE_CHECKINS + "/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    /* ================= helpers (mismo patron que LikesFotosIT) ================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Checkin IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("checkin-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-checkin-it");
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
        perfil.setNombre("Perfil Checkin IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0901");
        perfil.setWhatsappNormalizado("5492235550901");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para CheckinIT."));

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
                        "No hay una ciudad activa con barrio activo para CheckinIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Checkin IT");
        ubicacion.setDireccion("Calle checkin 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Checkin IT");
        actividad.setSlug(slugUnico("actividad-checkin-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por CheckinIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0902");
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
                        .subject("checkin-it@dondeentreno.test")
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
