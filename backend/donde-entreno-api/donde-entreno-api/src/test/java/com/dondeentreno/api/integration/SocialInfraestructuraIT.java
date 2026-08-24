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
import com.dondeentreno.api.repository.EventoInteraccionRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.ReporteRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Infraestructura social (script 28, Fase 2): notificación real al
 * seguir + campanita, reportes con cola admin, tracking anónimo que
 * llega a las métricas del publicador, y edición de datos del usuario.
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
class SocialInfraestructuraIT {

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
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private EventoInteraccionRepository eventoInteraccionRepository;

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
        for (Long usuarioId : usuarioIds) {
            notificacionRepository.deleteAll(
                    notificacionRepository.findAll().stream()
                            .filter(notificacion -> notificacion.getUsuarioId().equals(usuarioId))
                            .toList()
            );
            reporteRepository.deleteAll(
                    reporteRepository.findAll().stream()
                            .filter(reporte -> reporte.getUsuarioId().equals(usuarioId))
                            .toList()
            );
            seguimientoPublicadorRepository.deleteAll(
                    seguimientoPublicadorRepository.findByUsuario_IdOrderByCreatedAtDesc(usuarioId)
            );
        }
        notificacionRepository.flush();
        reporteRepository.flush();
        seguimientoPublicadorRepository.flush();

        for (Long actividadId : actividadIds) {
            eventoInteraccionRepository.deleteAll(
                    eventoInteraccionRepository.findAll().stream()
                            .filter(evento -> evento.getActividadId().equals(actividadId))
                            .toList()
            );
        }
        eventoInteraccionRepository.flush();

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
    void seguirEmiteNotificacionRealYLaCampanitaFunciona() throws Exception {
        Usuario seguidor = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Long duenioId = perfil.getUsuario().getId();

        /* El seguimiento real dispara la notificación al dueño. */
        mockMvc.perform(post("/api/usuario/seguimientos/publicadores/{id}", perfil.getId())
                        .with(jwtConRol(ROL_USUARIO, seguidor.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuario/notificaciones/contador")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(1));

        var respuesta = mockMvc.perform(get("/api/usuario/notificaciones")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("NUEVO_SEGUIDOR"))
                .andExpect(jsonPath("$.contenido[0].leida").value(false))
                .andReturn();

        long notificacionId = com.jayway.jsonpath.JsonPath
                .parse(respuesta.getResponse().getContentAsString())
                .<Number>read("$.contenido[0].id").longValue();

        mockMvc.perform(patch("/api/usuario/notificaciones/{id}/leida", notificacionId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuario/notificaciones/contador")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(jsonPath("$.noLeidas").value(0));
    }

    @Test
    void reportarLlegaALaColaDelAdminYSeResuelve() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_ADMIN);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        String cuerpo = "{\"tipoObjeto\":\"ACTIVIDAD\",\"objetoId\":" + actividad.getId()
                + ",\"motivo\":\"INFORMACION_FALSA\",\"detalle\":\"Los horarios no coinciden\"}";

        mockMvc.perform(post("/api/usuario/reportes")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNoContent());

        /* Repetido: idempotente, sin duplicar. */
        mockMvc.perform(post("/api/usuario/reportes")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNoContent());

        long filas = reporteRepository.findAll().stream()
                .filter(reporte -> reporte.getUsuarioId().equals(usuario.getId()))
                .count();
        assertEquals(1L, filas);

        var cola = mockMvc.perform(get("/api/admin/reportes?estado=PENDIENTE")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andReturn();

        long reporteId = com.jayway.jsonpath.JsonPath
                .parse(cola.getResponse().getContentAsString())
                .<Number>read("$.contenido[0].id").longValue();

        mockMvc.perform(patch("/api/admin/reportes/{id}/estado", reporteId)
                        .with(jwtConRol(ROL_ADMIN, admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"REVISADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REVISADO"));

        /* Un usuario común no ve la cola. */
        mockMvc.perform(get("/api/admin/reportes")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void elTrackingAnonimoLlegaALasMetricasDelPublicador() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        /* POST público, SIN sesión. */
        mockMvc.perform(post("/api/actividades/{id}/interacciones", actividad.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"VISTA_DETALLE\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/actividades/{id}/interacciones", actividad.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"CLICK_WHATSAPP\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/publicador/metricas")
                        .with(jwtConRol(ROL_PUBLICADOR, perfil.getUsuario().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vistas30Dias").value(1))
                .andExpect(jsonPath("$.contactosWhatsapp30Dias").value(1));
    }

    @Test
    void editarDatosDelUsuarioPersisteYSeRefleja() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);

        mockMvc.perform(patch("/api/usuario/perfil")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Agustín\",\"apellido\":\"Editado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Agustín"))
                .andExpect(jsonPath("$.apellido").value("Editado"));

        mockMvc.perform(get("/api/auth/me")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apellido").value("Editado"));

        /* Nombre vacío: 400 de validación. */
        mockMvc.perform(patch("/api/usuario/perfil")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \",\"apellido\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonimoDevuelve401EnLoPrivado() throws Exception {
        mockMvc.perform(get("/api/usuario/notificaciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/usuario/reportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/usuario/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/reportes")).andExpect(status().isUnauthorized());
    }

    /* ================= helpers (mismo patron que LikesFotosIT) ================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Social IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("social-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-social-it");
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
        perfil.setNombre("Perfil Social IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1101");
        perfil.setWhatsappNormalizado("5492235551101");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para SocialInfraestructuraIT."));

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
                        "No hay una ciudad activa con barrio activo para SocialInfraestructuraIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Social IT");
        ubicacion.setDireccion("Calle social 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Social IT");
        actividad.setSlug(slugUnico("actividad-social-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por SocialInfraestructuraIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-1102");
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
                        .subject("social-it@dondeentreno.test")
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
