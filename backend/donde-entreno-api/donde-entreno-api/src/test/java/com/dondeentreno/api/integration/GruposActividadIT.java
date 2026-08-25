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
import com.dondeentreno.api.repository.AvisoGrupoRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.ComentarioAvisoRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.MeGustaAvisoRepository;
import com.dondeentreno.api.repository.MiembroActividadRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Grupos por actividad (script 38).
 *
 * Con los casos de PRIVACIDAD, que es lo que define el bloque: el
 * contenido del grupo no sale del backend para quien no es miembro.
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
class GruposActividadIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String ROL_ADMIN = "ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private MiembroActividadRepository miembroRepository;

    @Autowired
    private AvisoGrupoRepository avisoRepository;

    @Autowired
    private ComentarioAvisoRepository comentarioRepository;

    @Autowired
    private MeGustaAvisoRepository meGustaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

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

    @BeforeEach
    void verificarDatasourceLocal() {
        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        for (Long usuarioId : usuarioIds) {
            notificacionRepository.deleteAll(
                    notificacionRepository.findAll().stream()
                            .filter(notificacion -> usuarioId.equals(notificacion.getUsuarioId()))
                            .toList());
        }
        notificacionRepository.flush();

        for (Long actividadId : actividadIds) {
            avisoRepository.findAll().stream()
                    .filter(aviso -> actividadId.equals(aviso.getActividadId()))
                    .forEach(aviso -> {
                        comentarioRepository.deleteAll(
                                comentarioRepository.findByAvisoIdOrderByCreatedAtAsc(aviso.getId()));
                        meGustaRepository.deleteAll(
                                meGustaRepository.findAll().stream()
                                        .filter(meGusta -> aviso.getId().equals(meGusta.getAvisoId()))
                                        .toList());
                    });
            comentarioRepository.flush();
            meGustaRepository.flush();

            avisoRepository.deleteAll(
                    avisoRepository.findAll().stream()
                            .filter(aviso -> actividadId.equals(aviso.getActividadId()))
                            .toList());

            miembroRepository.deleteAll(
                    miembroRepository.findAll().stream()
                            .filter(miembro -> actividadId.equals(miembro.getActividadId()))
                            .toList());
        }
        avisoRepository.flush();
        miembroRepository.flush();

        for (Long actividadId : actividadIds) {
            actividadRepository.findById(actividadId).ifPresent(actividadRepository::delete);
        }
        actividadRepository.flush();

        for (Long ubicacionId : ubicacionIds) {
            ubicacionRepository.findById(ubicacionId).ifPresent(ubicacionRepository::delete);
        }
        ubicacionRepository.flush();

        for (Long perfilPublicadorId : perfilPublicadorIds) {
            perfilPublicadorRepository.findById(perfilPublicadorId)
                    .ifPresent(perfilPublicadorRepository::delete);
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

    /** El camino completo del grupo, de punta a punta. */
    @Test
    void unirseVerElAvisoComentarYQueElPublicadorModere() throws Exception {
        Contexto contexto = crearContexto();
        Usuario miembro = crearUsuario(ROL_USUARIO);

        /* Antes de unirse: sin contenido. */
        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esMiembro").value(false))
                .andExpect(jsonPath("$.avisos", hasSize(0)));

        mockMvc.perform(put("/api/usuario/grupos/" + contexto.actividad().getId() + "/miembros")
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esMiembro").value(true))
                .andExpect(jsonPath("$.cantidadMiembros").value(1));

        /* El publicador avisa y le llega la campanita al miembro. */
        mockMvc.perform(post("/api/publicador/grupos/"
                        + contexto.actividad().getId() + "/avisos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Manana se suspende por lluvia\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isCreated());

        assertEquals(1, notificacionRepository.countByUsuarioIdAndLeidaFalse(miembro.getId()));

        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.avisos", hasSize(1)))
                .andExpect(jsonPath("$.avisos[0].texto")
                        .value("Manana se suspende por lluvia"));

        Long avisoId = avisoRepository.findAll().stream()
                .filter(aviso -> contexto.actividad().getId().equals(aviso.getActividadId()))
                .findFirst()
                .orElseThrow()
                .getId();

        /* El miembro comenta y reacciona. */
        mockMvc.perform(post("/api/usuario/grupos/avisos/" + avisoId + "/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Gracias por avisar!\"}")
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorNombre").value("Ana G."))
                .andExpect(jsonPath("$.esPropio").value(true));

        mockMvc.perform(put("/api/usuario/grupos/avisos/" + avisoId + "/me-gusta")
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.cantidadMeGusta").value(1));

        mockMvc.perform(get("/api/usuario/grupos/avisos/" + avisoId)
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.comentarios", hasSize(1)))
                .andExpect(jsonPath("$.cantidadMeGusta").value(1));

        /* El publicador modera su grupo. */
        Long comentarioId = comentarioRepository
                .findByAvisoIdOrderByCreatedAtAsc(avisoId)
                .get(0)
                .getId();

        mockMvc.perform(patch("/api/publicador/grupos/comentarios/"
                        + comentarioId + "/ocultar")
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuario/grupos/avisos/" + avisoId)
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.comentarios", hasSize(0)));
    }

    /**
     * PRIVACIDAD: el contenido del grupo NO sale para quien no es
     * miembro, ni siquiera para un admin.
     */
    @Test
    void elContenidoDelGrupoNoSaleParaQuienNoEsMiembro() throws Exception {
        Contexto contexto = crearContexto();
        Usuario miembro = crearUsuario(ROL_USUARIO);
        Usuario curioso = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_USUARIO);

        mockMvc.perform(put("/api/usuario/grupos/" + contexto.actividad().getId() + "/miembros")
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/publicador/grupos/"
                        + contexto.actividad().getId() + "/avisos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Algo solo para el grupo\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isCreated());

        /* Que el miembro SÍ lo vea: sin esto la ausencia no prueba nada. */
        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.avisos", hasSize(1)));

        /* El curioso ve la ficha, no el contenido. */
        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_USUARIO, curioso.getId())))
                .andExpect(jsonPath("$.esMiembro").value(false))
                .andExpect(jsonPath("$.avisos", hasSize(0)));

        /* Y un ADMIN tampoco: no hay endpoint que devuelva el grupo. */
        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(jsonPath("$.avisos", hasSize(0)));

        Long avisoId = avisoRepository.findAll().stream()
                .filter(aviso -> contexto.actividad().getId().equals(aviso.getActividadId()))
                .findFirst()
                .orElseThrow()
                .getId();

        /* Pedir el aviso directo tampoco: 404, ni se confirma que exista. */
        mockMvc.perform(get("/api/usuario/grupos/avisos/" + avisoId)
                        .with(jwtConRol(ROL_USUARIO, curioso.getId())))
                .andExpect(status().isNotFound());
    }

    /** Salir del grupo corta el acceso al contenido. */
    @Test
    void salirDelGrupoDejaDeMostrarLosAvisos() throws Exception {
        Contexto contexto = crearContexto();
        Usuario miembro = crearUsuario(ROL_USUARIO);

        mockMvc.perform(put("/api/usuario/grupos/" + contexto.actividad().getId() + "/miembros")
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/publicador/grupos/"
                        + contexto.actividad().getId() + "/avisos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Un aviso\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.avisos", hasSize(1)));

        mockMvc.perform(delete("/api/usuario/grupos/"
                        + contexto.actividad().getId() + "/miembros")
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuario/grupos/" + contexto.actividad().getId())
                        .with(jwtConRol(ROL_USUARIO, miembro.getId())))
                .andExpect(jsonPath("$.esMiembro").value(false))
                .andExpect(jsonPath("$.avisos", hasSize(0)));
    }

    @Test
    void elTercerAvisoDelDiaNoSePublica() throws Exception {
        Contexto contexto = crearContexto();

        for (String texto : List.of("Primero", "Segundo")) {
            mockMvc.perform(post("/api/publicador/grupos/"
                            + contexto.actividad().getId() + "/avisos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"texto\":\"" + texto + "\"}")
                            .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/publicador/grupos/"
                        + contexto.actividad().getId() + "/avisos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Tercero\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elGrupoExigeSesion() throws Exception {
        mockMvc.perform(get("/api/usuario/grupos/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/publicador/grupos/1/avisos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Sin sesion\"}"))
                .andExpect(status().isUnauthorized());
    }

    /* ======================= fixtures ======================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Ana");
        usuario.setApellido("Grupo");
        usuario.setEmail("grupo-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-grupo-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private Contexto crearContexto() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos."));

        Ciudad ciudad = ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .filter(cada -> !barrioRepository
                        .findByActivoTrueAndCiudad_IdOrderByNombreAsc(cada.getId()).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una ciudad activa con barrio activo."));

        Barrio barrio = barrioRepository
                .findByActivoTrueAndCiudad_IdOrderByNombreAsc(ciudad.getId())
                .get(0);

        Usuario duenio = crearUsuario(ROL_PUBLICADOR);
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Grupo IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-3801");
        perfil.setWhatsappNormalizado("5492235553801");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador perfilGuardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(perfilGuardado.getId());

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfilGuardado);
        ubicacion.setCiudad(ciudad);
        ubicacion.setBarrio(barrio);
        ubicacion.setNombre("Sede Grupo IT");
        ubicacion.setDireccion("Calle grupo 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfilGuardado);
        actividad.setDeporte(deporte);
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Grupo IT");
        actividad.setSlug(slugUnico("actividad-grupo-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por GruposActividadIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-3802");
        actividad.setEmailContacto(perfilGuardado.getEmailContacto());
        actividad.setEstadoPublicacion("PUBLICADA");
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad actividadGuardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(actividadGuardada.getId());

        return new Contexto(duenio, perfilGuardado, actividadGuardada);
    }

    private String slugUnico(String base) {
        return base.toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private record Contexto(Usuario duenio, PerfilPublicador perfil, Actividad actividad) {
    }

    private RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("grupo-it@dondeentreno.test")
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
