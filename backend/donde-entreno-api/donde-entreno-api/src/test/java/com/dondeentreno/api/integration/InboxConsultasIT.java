package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Conversacion;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.ConversacionRepository;
import com.dondeentreno.api.repository.MensajeRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
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
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inbox de consultas (script 36).
 *
 * Endpoint nuevo, IT propio. Y con casos específicos de PRIVACIDAD,
 * que es la decisión que ordenó todo el módulo: el admin no lee
 * conversaciones.
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
class InboxConsultasIT {

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
    private ConversacionRepository conversacionRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

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

        List<Conversacion> mias = conversacionRepository.findAll().stream()
                .filter(conversacion -> usuarioIds.contains(conversacion.getUsuarioId())
                        || perfilPublicadorIds.contains(conversacion.getPerfilPublicadorId()))
                .toList();

        for (Conversacion conversacion : mias) {
            mensajeRepository.deleteAll(
                    mensajeRepository.findByConversacionIdOrderByCreatedAtAsc(conversacion.getId()));
        }
        mensajeRepository.flush();
        conversacionRepository.deleteAll(mias);
        conversacionRepository.flush();

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

    /** El camino feliz completo, ida y vuelta. */
    @Test
    void consultarLlegaAlPublicadorYLaRespuestaVuelveAlUsuario() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario duenio = perfil.getUsuario();
        Usuario interesado = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Hola, ¿hay clases los sábados?");

        /* Le llegó a la bandeja del publicador, con el no leído. */
        mockMvc.perform(get("/api/publicador/consultas")
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].noLeidos").value(1))
                .andExpect(jsonPath("$[0].ultimoMensajeTexto")
                        .value("Hola, ¿hay clases los sábados?"));

        /* Y a su campanita. */
        assertEquals(1, notificacionRepository.countByUsuarioIdAndLeidaFalse(duenio.getId()));

        Long conversacionId = conversacionRepository
                .findByPerfilPublicadorIdOrderByUltimoMensajeAtDesc(perfil.getId())
                .get(0)
                .getId();

        /* Abrir el hilo marca leído: abrirlo ES haberlo leído. */
        mockMvc.perform(get("/api/publicador/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidos").value(0))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0].esPropio").value(false));

        /* Responde y le vuelve al usuario. */
        mockMvc.perform(post("/api/publicador/consultas/" + conversacionId + "/respuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Si, sabados de 10 a 12.\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuario/consultas")
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].noLeidos").value(1))
                .andExpect(jsonPath("$[0].contraparteNombre").value(perfil.getNombre()));

        assertEquals(1, notificacionRepository.countByUsuarioIdAndLeidaFalse(interesado.getId()));
    }

    /** Cerrar significa algo: el publicador deja de poder escribir. */
    @Test
    void cerradaPorElUsuarioElPublicadorNoPuedeResponder() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario interesado = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Consulta que despues cierro");
        Long conversacionId = conversacionRepository
                .findByUsuarioIdOrderByUltimoMensajeAtDesc(interesado.getId())
                .get(0)
                .getId();

        /* Que ANTES pueda: sin esto, el test pasaría aunque nunca pudiera. */
        mockMvc.perform(post("/api/publicador/consultas/" + conversacionId + "/respuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Hola!\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, perfil.getUsuario().getId())))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/usuario/consultas/" + conversacionId + "/cerrar")
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/publicador/consultas/" + conversacionId + "/respuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Otra cosa mas\"}")
                        .with(jwtConRol(ROL_PUBLICADOR, perfil.getUsuario().getId())))
                .andExpect(status().isBadRequest());
    }

    /** Un hilo ajeno da 404, no 403: no se delata que existe. */
    @Test
    void unUsuarioAjenoNoVeLaConversacion() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario interesado = crearUsuario(ROL_USUARIO);
        Usuario curioso = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Algo privado");
        Long conversacionId = conversacionRepository
                .findByUsuarioIdOrderByUltimoMensajeAtDesc(interesado.getId())
                .get(0)
                .getId();

        mockMvc.perform(get("/api/usuario/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_USUARIO, curioso.getId())))
                .andExpect(status().isNotFound());
    }

    /**
     * PRIVACIDAD — el corazón del módulo: ni siquiera un ADMIN puede
     * abrir un hilo. No hay endpoint que lo permita, y el del
     * publicador le responde 403 porque no tiene ese rol.
     */
    @Test
    void niSiquieraUnAdminPuedeAbrirElHiloCompleto() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario interesado = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Algo privado entre dos");
        Long conversacionId = conversacionRepository
                .findByUsuarioIdOrderByUltimoMensajeAtDesc(interesado.getId())
                .get(0)
                .getId();

        mockMvc.perform(get("/api/publicador/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuario/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isNotFound());
    }

    /**
     * PRIVACIDAD — lo único que el admin ve de un hilo reportado: el
     * mensaje y a lo sumo los dos anteriores, aunque haya más.
     */
    @Test
    void elContextoDeUnReporteNuncaTraeElHiloEntero() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario duenio = perfil.getUsuario();
        Usuario interesado = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Mensaje uno");
        Long conversacionId = conversacionRepository
                .findByUsuarioIdOrderByUltimoMensajeAtDesc(interesado.getId())
                .get(0)
                .getId();

        for (String texto : List.of("Mensaje dos", "Mensaje tres", "Mensaje cuatro")) {
            mockMvc.perform(post("/api/publicador/consultas/" + conversacionId + "/respuestas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"texto\":\"" + texto + "\"}")
                            .with(jwtConRol(ROL_PUBLICADOR, duenio.getId())))
                    .andExpect(status().isOk());
        }

        /* El hilo tiene CUATRO mensajes: lo confirma quien sí puede verlo. */
        mockMvc.perform(get("/api/usuario/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$.mensajes", hasSize(4)));

        Long ultimoId = mensajeRepository
                .findByConversacionIdOrderByCreatedAtAsc(conversacionId)
                .get(3)
                .getId();

        mockMvc.perform(get("/api/admin/mensajes/" + ultimoId + "/contexto")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].texto").value("Mensaje dos"));
    }

    /** El mensaje ocultado deja el hueco, pero el texto no vuelve. */
    @Test
    void ocultarUnMensajeLoSacaDelHiloSinBorrarlo() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario interesado = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Un exabrupto");
        Long conversacionId = conversacionRepository
                .findByUsuarioIdOrderByUltimoMensajeAtDesc(interesado.getId())
                .get(0)
                .getId();
        Long mensajeId = mensajeRepository
                .findByConversacionIdOrderByCreatedAtAsc(conversacionId)
                .get(0)
                .getId();

        mockMvc.perform(get("/api/usuario/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$.mensajes[0].texto").value("Un exabrupto"));

        mockMvc.perform(patch("/api/admin/mensajes/" + mensajeId + "/ocultar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuario/consultas/" + conversacionId)
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0].oculto").value(true))
                .andExpect(jsonPath("$.mensajes[0].texto").doesNotExist());
    }

    /** Escribirle dos veces al mismo club no abre dos hilos. */
    @Test
    void dosConsultasAlMismoClubVanAlMismoHilo() throws Exception {
        PerfilPublicador perfil = crearPerfilPublicador();
        Usuario interesado = crearUsuario(ROL_USUARIO);

        consultar(interesado, perfil, "Primera");
        consultar(interesado, perfil, "Segunda");

        assertEquals(1, conversacionRepository
                .findByUsuarioIdOrderByUltimoMensajeAtDesc(interesado.getId())
                .size());
    }

    @Test
    void elInboxExigeSesion() throws Exception {
        mockMvc.perform(get("/api/usuario/consultas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/publicador/consultas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/mensajes/1/contexto")).andExpect(status().isUnauthorized());
    }

    /* ======================= fixtures ======================= */

    private void consultar(Usuario usuario, PerfilPublicador perfil, String texto)
            throws Exception {
        mockMvc.perform(post("/api/usuario/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perfilPublicadorId\":\"" + perfil.getId()
                                + "\",\"texto\":\"" + texto + "\"}")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isCreated());
    }

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Ana");
        usuario.setApellido("Inbox");
        usuario.setEmail("inbox-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-inbox-it");
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
                .orElseThrow(() -> new IllegalStateException("No hay ciudades activas para InboxConsultasIT."));
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Inbox IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-2001");
        perfil.setWhatsappNormalizado("5492235552001");
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
                        .subject("inbox-it@dondeentreno.test")
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
