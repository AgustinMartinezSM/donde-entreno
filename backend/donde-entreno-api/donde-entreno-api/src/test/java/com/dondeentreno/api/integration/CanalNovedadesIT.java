package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Novedad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.FeedEventRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
import com.dondeentreno.api.repository.NovedadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.SeguimientoPublicadorRepository;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Canal de novedades del publicador (script 34, Fase 8).
 *
 * Endpoint nuevo, IT propio (regla que dejó la Fase 7): los unitarios
 * mockean los repositorios y nunca ejecutan la query real ni el
 * afterCommit del feed.
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
class CanalNovedadesIT {

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
    private NovedadRepository novedadRepository;

    @Autowired
    private FeedEventRepository feedEventRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private SeguimientoPublicadorRepository seguimientoPublicadorRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

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

        for (Long perfilId : perfilPublicadorIds) {
            feedEventRepository.deleteAll(
                    feedEventRepository.findAll().stream()
                            .filter(evento -> perfilId.equals(evento.getPerfilPublicadorId()))
                            .toList());
        }
        feedEventRepository.flush();

        for (Long perfilId : perfilPublicadorIds) {
            novedadRepository.deleteAll(
                    novedadRepository.findAll().stream()
                            .filter(novedad -> perfilId.equals(novedad.getPerfilPublicadorId()))
                            .toList());
        }
        novedadRepository.flush();

        for (Long usuarioId : usuarioIds) {
            seguimientoPublicadorRepository.deleteAll(
                    seguimientoPublicadorRepository.findAll().stream()
                            .filter(seguimiento -> seguimiento.getUsuario() != null
                                    && usuarioId.equals(seguimiento.getUsuario().getId()))
                            .toList());
        }
        seguimientoPublicadorRepository.flush();

        for (Long perfilPublicadorId : perfilPublicadorIds) {
            perfilPublicadorRepository.findById(perfilPublicadorId)
                    .ifPresent(perfilPublicadorRepository::delete);
        }
        perfilPublicadorRepository.flush();

        for (Long usuarioId : usuarioIds) {
            usuarioRepository.findById(usuarioId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();

        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }

    /**
     * El camino feliz completo de la fase: publicar llega al perfil
     * público, al feed del seguidor y a su campanita, todo de una.
     */
    @Test
    void publicarUnaNovedadLlegaAlPerfilAlFeedYALaCampanita() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario duenio = perfil.getUsuario();
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, perfil);

        mockMvc.perform(post("/api/publicador/novedades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Cambiamos el horario del sabado a las 10\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Cambiamos el horario del sabado a las 10"))
                .andExpect(jsonPath("$.perfilNombre").value(perfil.getNombre()));

        /* En el perfil público, sin sesión. */
        mockMvc.perform(get("/api/perfiles-publicadores/" + perfil.getId() + "/novedades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].texto").value("Cambiamos el horario del sabado a las 10"));

        /* En el feed del seguidor, con el texto completo resuelto. */
        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].tipo").value("NOVEDAD"))
                .andExpect(jsonPath("$.contenido[0].novedadTexto")
                        .value("Cambiamos el horario del sabado a las 10"));

        /* Y la campanita: una notificación para el seguidor. */
        assertEquals(1, notificacionRepository.countByUsuarioIdAndLeidaFalse(seguidor.getId()));
    }

    /**
     * Ocultarla la saca de TODOS lados, no solo del perfil: el feed ya
     * tenía su evento emitido y también tiene que dejar de mostrarla.
     */
    @Test
    void ocultarlaPorAdminLaSacaDelPerfilYDelFeed() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, perfil);

        publicar(perfil.getUsuario(), "Quedan 3 lugares para el turno de la tarde");
        Long novedadId = ultimaNovedadDe(perfil).getId();

        /*
          Que ANTES esté: sin esta aserción, el test pasaría igual si el
          evento nunca se hubiera emitido — que es exactamente el bug
          que se escondía detrás de este mismo caso.
        */
        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.contenido", hasSize(1)));

        mockMvc.perform(patch("/api/admin/novedades/" + novedadId + "/ocultar")
                        .with(jwtConRol(ROL_ADMIN, crearUsuario(ROL_USUARIO).getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/perfiles-publicadores/" + perfil.getId() + "/novedades"))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    /** El publicador la borra: baja lógica, fuera del perfil público. */
    @Test
    void elPublicadorPuedeBorrarLaSuyaYDejaDeVerseAfuera() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario duenio = perfil.getUsuario();

        publicar(duenio, "Nos mudamos de sede la semana que viene");
        Long novedadId = ultimaNovedadDe(perfil).getId();

        mockMvc.perform(delete("/api/publicador/novedades/" + novedadId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/perfiles-publicadores/" + perfil.getId() + "/novedades"))
                .andExpect(jsonPath("$", hasSize(0)));

        /* En su panel tampoco: solo lo no eliminado. */
        mockMvc.perform(get("/api/publicador/novedades")
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * El tope diario es del canal, no del que lee: sin él un publicador
     * puede inundar el feed de todos sus seguidores.
     */
    @Test
    void laCuartaDelDiaNoSePublica() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario duenio = perfil.getUsuario();

        publicar(duenio, "Primera del dia");
        publicar(duenio, "Segunda del dia");
        publicar(duenio, "Tercera del dia");

        mockMvc.perform(post("/api/publicador/novedades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Cuarta del dia\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isBadRequest());

        assertEquals(3, novedadRepository
                .findByPerfilPublicadorIdAndEstadoNotOrderByCreatedAtDesc(
                        perfil.getId(), "ELIMINADA_POR_PUBLICADOR")
                .size());
    }

    /**
     * Solo la PRIMERA del día avisa. Tres novedades por cincuenta
     * seguidores serían ciento cincuenta avisos a la misma gente.
     */
    @Test
    void soloLaPrimeraDelDiaTocaLaCampanita() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, perfil);

        publicar(perfil.getUsuario(), "Primera del dia");
        publicar(perfil.getUsuario(), "Segunda del dia");

        assertEquals(1, notificacionRepository.countByUsuarioIdAndLeidaFalse(seguidor.getId()));
    }

    /**
     * Reacciones (script 37): suman, son idempotentes, se ven en el
     * perfil público y en el feed, y se pueden quitar.
     */
    @Test
    void reaccionarSumaSeVeEnLasDosSuperficiesYSePuedeQuitar() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, perfil);

        publicar(perfil.getUsuario(), "Una novedad para reaccionar");
        Long novedadId = ultimaNovedadDe(perfil).getId();

        /* Antes de reaccionar: cero y sin marcar. */
        mockMvc.perform(get("/api/perfiles-publicadores/" + perfil.getId() + "/novedades")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$[0].cantidadMeGusta").value(0))
                .andExpect(jsonPath("$[0].meGusta").value(false));

        mockMvc.perform(put("/api/usuario/novedades/" + novedadId + "/me-gusta")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadMeGusta").value(1));

        /* Repetir no duplica. */
        mockMvc.perform(put("/api/usuario/novedades/" + novedadId + "/me-gusta")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.cantidadMeGusta").value(1));

        /* Se ve en el perfil, con su sesión. */
        mockMvc.perform(get("/api/perfiles-publicadores/" + perfil.getId() + "/novedades")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$[0].cantidadMeGusta").value(1))
                .andExpect(jsonPath("$[0].meGusta").value(true));

        /* Y en el feed, que es la otra superficie donde se ven. */
        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.contenido[0].novedadMeGusta").value(1))
                .andExpect(jsonPath("$.contenido[0].novedadMeGustaPropio").value(true));

        /* Anónimo ve el contador pero no un "meGusta" ajeno. */
        mockMvc.perform(get("/api/perfiles-publicadores/" + perfil.getId() + "/novedades"))
                .andExpect(jsonPath("$[0].cantidadMeGusta").value(1))
                .andExpect(jsonPath("$[0].meGusta").value(false));

        mockMvc.perform(delete("/api/usuario/novedades/" + novedadId + "/me-gusta")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.cantidadMeGusta").value(0));
    }

    @Test
    void reaccionarExigeSesion() throws Exception {
        mockMvc.perform(put("/api/usuario/novedades/1/me-gusta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicarExigeSesionDePublicador() throws Exception {
        mockMvc.perform(post("/api/publicador/novedades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Sin sesion\"}"))
                .andExpect(status().isUnauthorized());
    }

    /* ======================= fixtures ======================= */

    private void publicar(Usuario duenio, String texto) throws Exception {
        mockMvc.perform(post("/api/publicador/novedades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"" + texto + "\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isCreated());
    }

    private Novedad ultimaNovedadDe(PerfilPublicador perfil) {
        return novedadRepository
                .findByPerfilPublicadorIdAndEstadoNotOrderByCreatedAtDesc(
                        perfil.getId(), "ELIMINADA_POR_PUBLICADOR")
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se guardo la novedad."));
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
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Novedades IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("novedades-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-novedades-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private PerfilPublicador crearPerfilPublicador() {
        Usuario duenio = crearUsuario(ROL_PUBLICADOR);
        Ciudad ciudad = ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay ciudades activas para CanalNovedadesIT."));
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Novedades IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1801");
        perfil.setWhatsappNormalizado("5492235551801");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador guardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(guardado.getId());
        return guardado;
    }

    private RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("novedades-it@dondeentreno.test")
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
