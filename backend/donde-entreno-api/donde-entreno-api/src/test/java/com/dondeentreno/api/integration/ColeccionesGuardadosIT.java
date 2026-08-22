package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.ColeccionGuardados;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.ColeccionGuardadosRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.FavoritoActividadRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de colecciones de guardados (script 22, bloque 13):
 * crear, organizar un guardado con coleccion y nota, listar organizado,
 * y el contrato central — borrar la coleccion devuelve los guardados a
 * "Todos" SIN borrarlos (ON DELETE SET NULL de la base, no codigo).
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
class ColeccionesGuardadosIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String BASE_COLECCIONES = "/api/usuario/colecciones";
    private static final String BASE_FAVORITOS = "/api/usuario/favoritos";

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
    private FavoritoActividadRepository favoritoActividadRepository;

    @Autowired
    private ColeccionGuardadosRepository coleccionGuardadosRepository;

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

    /* Colecciones y favoritos caen por CASCADE al borrar usuarios. */
    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        for (Long usuarioId : usuarioIds) {
            favoritoActividadRepository.deleteAll(
                    favoritoActividadRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)
            );
            coleccionGuardadosRepository.deleteAll(
                    coleccionGuardadosRepository.findByUsuarioIdOrderByNombreAsc(usuarioId)
            );
        }
        favoritoActividadRepository.flush();
        coleccionGuardadosRepository.flush();

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
    void crearOrganizarFiltrarYBorrarSinPerderGuardados() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        mockMvc.perform(put(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        /* Crear la coleccion. */
        String cuerpoColeccion = mockMvc.perform(post(BASE_COLECCIONES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Para probar\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Para probar"))
                .andExpect(jsonPath("$.cantidad").value(0))
                .andReturn().getResponse().getContentAsString();
        long coleccionId = objectMapper.readTree(cuerpoColeccion).get("id").asLong();

        /* Nombre duplicado (otras mayusculas): 400. */
        mockMvc.perform(post(BASE_COLECCIONES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"PARA PROBAR\"}"))
                .andExpect(status().isBadRequest());

        /* Organizar el guardado: coleccion + nota. */
        mockMvc.perform(patch(BASE_FAVORITOS + "/{slug}", actividad.getSlug())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coleccionId\": " + coleccionId + ", \"nota\": \"preguntar horario\"}"))
                .andExpect(status().isNoContent());

        /* El listado organizado trae card + coleccion + nota, y el conteo subio. */
        mockMvc.perform(get(BASE_FAVORITOS + "/organizados")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actividad.slug").value(actividad.getSlug()))
                .andExpect(jsonPath("$[0].coleccionId").value(coleccionId))
                .andExpect(jsonPath("$[0].nota").value("preguntar horario"));
        mockMvc.perform(get(BASE_COLECCIONES)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidad").value(1));

        /* EL CONTRATO CENTRAL: borrar la coleccion no borra el guardado. */
        mockMvc.perform(delete(BASE_COLECCIONES + "/{id}", coleccionId)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_FAVORITOS + "/organizados")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].coleccionId").doesNotExist())
                .andExpect(jsonPath("$[0].nota").value("preguntar horario"));

        assertEquals(1, favoritoActividadRepository
                .findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).size());
    }

    @Test
    void unaColeccionAjenaDa404YNoSePuedeUsar() throws Exception {
        Usuario duenio = crearUsuario(ROL_USUARIO);
        Usuario intruso = crearUsuario(ROL_USUARIO);

        ColeccionGuardados coleccion = new ColeccionGuardados();
        coleccion.setUsuarioId(duenio.getId());
        coleccion.setNombre("Privada");
        coleccion.setCreatedAt(OffsetDateTime.now());
        ColeccionGuardados guardada = coleccionGuardadosRepository.saveAndFlush(coleccion);

        mockMvc.perform(patch(BASE_COLECCIONES + "/{id}", guardada.getId())
                        .with(jwtConRol(ROL_USUARIO, intruso.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Robada\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(BASE_COLECCIONES + "/{id}", guardada.getId())
                        .with(jwtConRol(ROL_USUARIO, intruso.getId())))
                .andExpect(status().isNotFound());

        /* Y el listado del intruso no la muestra. */
        mockMvc.perform(get(BASE_COLECCIONES)
                        .with(jwtConRol(ROL_USUARIO, intruso.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anonimoDevuelve401EnTodo() throws Exception {
        mockMvc.perform(get(BASE_COLECCIONES)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE_COLECCIONES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_FAVORITOS + "/organizados")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE_FAVORITOS + "/{slug}", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coleccionId\": null, \"nota\": null}"))
                .andExpect(status().isUnauthorized());
    }

    /* ================= helpers (mismo patron que SyncFavoritosIT) ================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Colecciones IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("colecciones-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-colecciones-it");
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
        perfil.setNombre("Perfil Colecciones IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0701");
        perfil.setWhatsappNormalizado("5492235550701");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para ColeccionesGuardadosIT."));

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
                        "No hay una ciudad activa con barrio activo para ColeccionesGuardadosIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Colecciones IT");
        ubicacion.setDireccion("Calle colecciones 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Colecciones IT");
        actividad.setSlug(slugUnico("actividad-colecciones-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por ColeccionesGuardadosIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0702");
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
                        .subject("colecciones-it@dondeentreno.test")
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
