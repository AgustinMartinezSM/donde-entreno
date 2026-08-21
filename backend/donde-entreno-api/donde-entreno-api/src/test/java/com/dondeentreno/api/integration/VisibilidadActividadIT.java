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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT del pausar/reanudar (fase 6) contra PostgreSQL local: pausar saca
 * la actividad del publico y de las listas de favoritos SIN borrar
 * nada, el panel del dueño la sigue viendo y gestionando, y reanudar
 * devuelve todo. Ajenos, anonimos y usuarios comunes no pueden tocarla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
/*
  Mismos valores que RefreshTokenIT/CambiarPasswordIT A PROPOSITO: cada
  combinacion distinta de propiedades crea OTRO contexto de Spring con
  OTRO pool contra el Postgres local, y el cache de contextos ya hizo
  fallar el arranque del ultimo en llegar (EntityManagerFactory sin
  conexiones). Valores identicos = contexto compartido.
*/
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-integration-local-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class VisibilidadActividadIT {

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
    void pausarSacaDelPublicoYDeFavoritosSinBorrarYReanudarDevuelveTodo() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Long duenioId = perfil.getUsuario().getId();
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        /* Antes de pausar: publica visible y favorito guardado. */
        mockMvc.perform(get("/api/actividades/{slug}", actividad.getSlug()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/usuario/favoritos/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        /* Pausar: 200 con el detalle actualizado. */
        mockMvc.perform(patch("/api/publicador/actividades/{id}/visibilidad", actividad.getId())
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("PAUSADA"));

        /* El publico ya no la ve. */
        mockMvc.perform(get("/api/actividades/{slug}", actividad.getSlug()))
                .andExpect(status().isNotFound());

        /* La lista de favoritos la omite, pero la fila queda. */
        mockMvc.perform(get("/api/usuario/favoritos")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        assertEquals(1, favoritoActividadRepository
                .findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).size());

        /* El panel del dueño la sigue viendo, con su estado real. */
        mockMvc.perform(get("/api/publicador/actividades")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estadoPublicacion").value("PAUSADA"));
        mockMvc.perform(get("/api/publicador/actividades/{id}", actividad.getId())
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(status().isOk());

        /* Pausar lo pausado es idempotente. */
        mockMvc.perform(patch("/api/publicador/actividades/{id}/visibilidad", actividad.getId())
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("PAUSADA"));

        /* Reanudar: todo vuelve, favorito incluido. */
        mockMvc.perform(patch("/api/publicador/actividades/{id}/visibilidad", actividad.getId())
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"));

        mockMvc.perform(get("/api/actividades/{slug}", actividad.getSlug()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuario/favoritos")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value(actividad.getSlug()));
    }

    @Test
    void unPublicadorAjenoNoPuedePausarla() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador duenio = crearPerfilPublicador(referencias.ciudad());
        PerfilPublicador ajeno = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(duenio, referencias);

        mockMvc.perform(patch("/api/publicador/actividades/{id}/visibilidad", actividad.getId())
                        .with(jwtConRol(ROL_PUBLICADOR, ajeno.getUsuario().getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isNotFound());

        /* Y la actividad sigue publicada. */
        mockMvc.perform(get("/api/actividades/{slug}", actividad.getSlug()))
                .andExpect(status().isOk());
    }

    @Test
    void anonimoYUsuarioComunNoPuedenTocarVisibilidad() throws Exception {
        Usuario usuarioComun = crearUsuario(ROL_USUARIO);

        mockMvc.perform(patch("/api/publicador/actividades/{id}/visibilidad", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/publicador/actividades/{id}/visibilidad", 1L)
                        .with(jwtConRol(ROL_USUARIO, usuarioComun.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isForbidden());
    }

    /* ================= helpers (mismo patron que SyncFavoritosIT) ================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Visibilidad IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("visibilidad-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-visibilidad-it");
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
        perfil.setNombre("Perfil Visibilidad IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0601");
        perfil.setWhatsappNormalizado("5492235550601");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para VisibilidadActividadIT."));

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
                        "No hay una ciudad activa con barrio activo para VisibilidadActividadIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Visibilidad IT");
        ubicacion.setDireccion("Calle visibilidad 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Visibilidad IT");
        actividad.setSlug(slugUnico("actividad-visibilidad-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por VisibilidadActividadIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0602");
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
                        .subject("visibilidad-it@dondeentreno.test")
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
