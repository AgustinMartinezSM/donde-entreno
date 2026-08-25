package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.EventoDeportivo;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SeguimientoPublicador;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.EventoDeportivoRepository;
import com.dondeentreno.api.repository.FeedEventRepository;
import com.dondeentreno.api.repository.InteresEventoRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
 * Eventos y calendario (script 35, Fase 9).
 *
 * Endpoint nuevo, IT propio (regla de la Fase 7). Y ejerce el camino
 * REAL de emisión del feed —publicando por HTTP, no insertando
 * `feed_event` a mano— que es justo por donde se escapó el bug de la
 * Fase 6.
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
class EventosCalendarioIT {

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
    private EventoDeportivoRepository eventoRepository;

    @Autowired
    private InteresEventoRepository interesEventoRepository;

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

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

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

        for (Long perfilId : perfilPublicadorIds) {
            feedEventRepository.deleteAll(
                    feedEventRepository.findAll().stream()
                            .filter(evento -> perfilId.equals(evento.getPerfilPublicadorId()))
                            .toList());
        }
        feedEventRepository.flush();

        for (Long perfilId : perfilPublicadorIds) {
            List<EventoDeportivo> mios = eventoRepository.findAll().stream()
                    .filter(evento -> perfilId.equals(evento.getPerfilPublicadorId()))
                    .toList();

            for (EventoDeportivo evento : mios) {
                interesEventoRepository.deleteAll(
                        interesEventoRepository.findAll().stream()
                                .filter(interes -> evento.getId().equals(interes.getEventoDeportivoId()))
                                .toList());
            }
            interesEventoRepository.flush();
            eventoRepository.deleteAll(mios);
        }
        eventoRepository.flush();

        for (Long usuarioId : usuarioIds) {
            seguimientoPublicadorRepository.deleteAll(
                    seguimientoPublicadorRepository.findAll().stream()
                            .filter(seguimiento -> seguimiento.getUsuario() != null
                                    && usuarioId.equals(seguimiento.getUsuario().getId()))
                            .toList());
        }
        seguimientoPublicadorRepository.flush();

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

        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }

    /**
     * El camino feliz completo: publicar llega al calendario público,
     * al detalle, al feed del seguidor y a su campanita.
     */
    @Test
    void publicarUnEventoLlegaAlCalendarioAlFeedYALaCampanita() throws Exception {
        Contexto contexto = crearContexto();
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, contexto.perfil());

        String slug = publicar(contexto, "Torneo de verano", OffsetDateTime.now().plusDays(3));

        /* En el calendario público, sin sesión. */
        mockMvc.perform(get("/api/eventos").param("rango", "proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Torneo de verano"))
                .andExpect(jsonPath("$.contenido[0].ciudadNombre")
                        .value(contexto.ciudad().getNombre()))
                .andExpect(jsonPath("$.contenido[0].deporteNombre")
                        .value(contexto.deporte().getNombre()))
                .andExpect(jsonPath("$.contenido[0].cantidadInteresados").value(0));

        /* El detalle por slug. */
        mockMvc.perform(get("/api/eventos/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilNombre").value(contexto.perfil().getNombre()))
                .andExpect(jsonPath("$.sedeNombre").value("Sede Eventos IT"));

        /* En el feed del seguidor, con la fecha resuelta. */
        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].tipo").value("EVENTO_NUEVO"))
                .andExpect(jsonPath("$.contenido[0].eventoTitulo").value("Torneo de verano"))
                .andExpect(jsonPath("$.contenido[0].eventoSlug").value(slug));

        /* Y la campanita. */
        assertEquals(1, notificacionRepository.countByUsuarioIdAndLeidaFalse(seguidor.getId()));
    }

    /**
     * Cancelar NO es borrar: sale del calendario y del feed, pero el
     * detalle sigue vivo diciendo que se canceló, porque el link ya
     * circuló por WhatsApp.
     */
    @Test
    void cancelarLoSacaDelCalendarioPeroDejaVivoElDetalle() throws Exception {
        Contexto contexto = crearContexto();
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        seguir(seguidor, contexto.perfil());

        String slug = publicar(contexto, "Clase abierta", OffsetDateTime.now().plusDays(2));
        Long eventoId = eventoRepository.findBySlug(slug).orElseThrow().getId();

        /* Que ANTES esté: una desaparición solo prueba algo si hubo aparición. */
        mockMvc.perform(get("/api/eventos"))
                .andExpect(jsonPath("$.contenido", hasSize(1)));
        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.contenido", hasSize(1)));

        mockMvc.perform(patch("/api/publicador/eventos/" + eventoId + "/cancelar")
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/eventos"))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
        mockMvc.perform(get("/api/usuario/feed")
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        /* El detalle sobrevive y lo dice. */
        mockMvc.perform(get("/api/eventos/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    /** Borrar sí lo saca de todos lados, detalle incluido. */
    @Test
    void borrarloLoSacaTambienDelDetalle() throws Exception {
        Contexto contexto = crearContexto();

        String slug = publicar(contexto, "Seminario", OffsetDateTime.now().plusDays(4));
        Long eventoId = eventoRepository.findBySlug(slug).orElseThrow().getId();

        mockMvc.perform(get("/api/eventos/" + slug)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/publicador/eventos/" + eventoId)
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/eventos/" + slug)).andExpect(status().isNotFound());
    }

    /** "Me interesa" suma, es idempotente y se puede sacar. */
    @Test
    void meInteresaEsIdempotenteYSeRefleja() throws Exception {
        Contexto contexto = crearContexto();
        Usuario interesado = crearUsuario(ROL_USUARIO);

        String slug = publicar(contexto, "Maraton", OffsetDateTime.now().plusDays(6));
        Long eventoId = eventoRepository.findBySlug(slug).orElseThrow().getId();

        mockMvc.perform(put("/api/usuario/eventos/" + eventoId + "/interes")
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadInteresados").value(1));

        /* Repetir no duplica. */
        mockMvc.perform(put("/api/usuario/eventos/" + eventoId + "/interes")
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$.cantidadInteresados").value(1));

        /* Y el detalle, con su sesión, sabe que ya lo marcó. */
        mockMvc.perform(get("/api/eventos/" + slug)
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$.cantidadInteresados").value(1))
                .andExpect(jsonPath("$.meInteresa").value(true));

        mockMvc.perform(delete("/api/usuario/eventos/" + eventoId + "/interes")
                        .with(jwtConRol(ROL_USUARIO, interesado.getId())))
                .andExpect(jsonPath("$.cantidadInteresados").value(0));
    }

    /**
     * El rango se resuelve en el backend y filtra de verdad: "hoy" no
     * puede traer el torneo de la semana que viene.
     */
    @Test
    void elRangoHoyNoTraeLoDeLaSemanaQueViene() throws Exception {
        Contexto contexto = crearContexto();

        publicar(contexto, "Dentro de una semana", OffsetDateTime.now().plusDays(7));

        mockMvc.perform(get("/api/eventos").param("rango", "hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        mockMvc.perform(get("/api/eventos").param("rango", "proximos"))
                .andExpect(jsonPath("$.contenido", hasSize(1)));
    }

    /** El evento aparece en la solapa del perfil público. */
    @Test
    void elEventoApareceEnElPerfilPublicoDelPublicador() throws Exception {
        Contexto contexto = crearContexto();

        publicar(contexto, "Exhibicion", OffsetDateTime.now().plusDays(5));

        mockMvc.perform(get("/api/perfiles-publicadores/"
                        + contexto.perfil().getId() + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Exhibicion"));
    }

    /** El admin lo oculta: se cae del calendario y del detalle. */
    @Test
    void ocultarloPorAdminLoSacaDeCirculacion() throws Exception {
        Contexto contexto = crearContexto();

        String slug = publicar(contexto, "Charla tecnica", OffsetDateTime.now().plusDays(3));
        Long eventoId = eventoRepository.findBySlug(slug).orElseThrow().getId();

        mockMvc.perform(get("/api/eventos")).andExpect(jsonPath("$.contenido", hasSize(1)));

        mockMvc.perform(patch("/api/admin/eventos/" + eventoId + "/ocultar")
                        .with(jwtConRol(ROL_ADMIN, crearUsuario(ROL_USUARIO).getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/eventos")).andExpect(jsonPath("$.contenido", hasSize(0)));
        mockMvc.perform(get("/api/eventos/" + slug)).andExpect(status().isNotFound());
    }

    /** Un evento con fecha pasada es un error de carga, no un dato. */
    @Test
    void unEventoConFechaPasadaNoSePublica() throws Exception {
        Contexto contexto = crearContexto();

        mockMvc.perform(post("/api/publicador/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Ya paso", OffsetDateTime.now().minusDays(1), contexto))
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elCalendarioEsPublicoPeroPublicarExigeSesion() throws Exception {
        mockMvc.perform(get("/api/eventos")).andExpect(status().isOk());

        mockMvc.perform(post("/api/publicador/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sin sesion\"}"))
                .andExpect(status().isUnauthorized());
    }

    /* ======================= fixtures ======================= */

    private String publicar(Contexto contexto, String titulo, OffsetDateTime inicia) throws Exception {
        String respuesta = mockMvc.perform(post("/api/publicador/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento(titulo, inicia, contexto))
                        .with(jwtConRol(ROL_PUBLICADOR, contexto.duenio().getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int desde = respuesta.indexOf("\"slug\":\"") + 8;
        return respuesta.substring(desde, respuesta.indexOf('"', desde));
    }

    private String cuerpoEvento(String titulo, OffsetDateTime inicia, Contexto contexto) {
        return "{"
                + "\"titulo\":\"" + titulo + "\","
                + "\"descripcion\":\"Un evento creado por EventosCalendarioIT.\","
                + "\"iniciaAt\":\"" + inicia.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\","
                + "\"ubicacionId\":\"" + contexto.ubicacion().getId() + "\","
                + "\"deporteId\":\"" + contexto.deporte().getId() + "\""
                + "}";
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
        usuario.setNombre("Usuario Eventos IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("eventos-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-eventos-it");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para EventosCalendarioIT."));

        Ciudad ciudad = ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .filter(cada -> !barrioRepository
                        .findByActivoTrueAndCiudad_IdOrderByNombreAsc(cada.getId()).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una ciudad activa con barrio activo para EventosCalendarioIT."));

        Barrio barrio = barrioRepository
                .findByActivoTrueAndCiudad_IdOrderByNombreAsc(ciudad.getId())
                .get(0);

        Usuario duenio = crearUsuario(ROL_PUBLICADOR);
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Eventos IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1901");
        perfil.setWhatsappNormalizado("5492235551901");
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
        ubicacion.setNombre("Sede Eventos IT");
        ubicacion.setDireccion("Calle eventos 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        return new Contexto(duenio, perfilGuardado, ubicacionGuardada, ciudad, deporte);
    }

    private record Contexto(
            Usuario duenio,
            PerfilPublicador perfil,
            Ubicacion ubicacion,
            Ciudad ciudad,
            Deporte deporte
    ) {
    }

    private RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("eventos-it@dondeentreno.test")
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
