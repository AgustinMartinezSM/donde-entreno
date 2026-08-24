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
import com.dondeentreno.api.repository.InteresActividadRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.PreguntaActividadRepository;
import com.dondeentreno.api.repository.ReporteRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UbicacionRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.repository.ValoracionRepository;
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
 * Fase 3 (script 29): el flujo quiero probar → ya probé → valorar
 * verificada, el promedio oculto con N<3, preguntas con respuesta del
 * dueño y notificaciones, y la moderación (reporte + ocultar admin).
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
class ConfianzaIT {

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
    private InteresActividadRepository interesActividadRepository;

    @Autowired
    private ValoracionRepository valoracionRepository;

    @Autowired
    private PreguntaActividadRepository preguntaActividadRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ReporteRepository reporteRepository;

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
            interesActividadRepository.deleteAll(
                    interesActividadRepository.findAll().stream()
                            .filter(interes -> interes.getUsuarioId().equals(usuarioId))
                            .toList());
            valoracionRepository.deleteAll(
                    valoracionRepository.findAll().stream()
                            .filter(valoracion -> valoracion.getUsuarioId().equals(usuarioId))
                            .toList());
            preguntaActividadRepository.deleteAll(
                    preguntaActividadRepository.findAll().stream()
                            .filter(pregunta -> pregunta.getUsuarioId().equals(usuarioId))
                            .toList());
            notificacionRepository.deleteAll(
                    notificacionRepository.findAll().stream()
                            .filter(notificacion -> notificacion.getUsuarioId().equals(usuarioId))
                            .toList());
            reporteRepository.deleteAll(
                    reporteRepository.findAll().stream()
                            .filter(reporte -> reporte.getUsuarioId().equals(usuarioId))
                            .toList());
        }
        interesActividadRepository.flush();
        valoracionRepository.flush();
        preguntaActividadRepository.flush();
        notificacionRepository.flush();
        reporteRepository.flush();

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
    void flujoQuieroProbarYaProbeYValoracionVerificada() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        /* Sin señal: valorar da 400 con mensaje claro. */
        mockMvc.perform(put("/api/usuario/valoraciones/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":5}"))
                .andExpect(status().isBadRequest());

        /* Quiero probar: cuenta en el social proof del detalle. */
        mockMvc.perform(put("/api/usuario/intereses/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"QUIERO_PROBAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("QUIERO_PROBAR"));

        mockMvc.perform(get("/api/actividades/{slug}/detalle", actividad.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socialProof.cantidadQuierenProbar").value(1));

        /* Con señal débil valora, sin insignia. */
        mockMvc.perform(put("/api/usuario/valoraciones/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":4,\"comentario\":\"Muy buena onda\",\"tags\":[\"BUEN_AMBIENTE\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificada").value(false));

        /* Ya probé → editar la valoración la vuelve verificada. */
        mockMvc.perform(put("/api/usuario/intereses/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"YA_PROBE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/usuario/valoraciones/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":5,\"comentario\":\"Confirmo: excelente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificada").value(true));

        /* Resumen público SIN sesión: 1 visible, promedio null (N<3). */
        mockMvc.perform(get("/api/actividades/{id}/valoraciones", actividad.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.promedio").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].comentario").value("Confirmo: excelente"))
                .andExpect(jsonPath("$.contenido[0].verificada").value(true));
    }

    @Test
    void preguntasConRespuestaDelDuenioYNotificaciones() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Long duenioId = perfil.getUsuario().getId();

        var creada = mockMvc.perform(post("/api/usuario/preguntas")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadId\":\"" + actividad.getId()
                                + "\",\"pregunta\":\"¿Hay clase de prueba?\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long preguntaId = com.jayway.jsonpath.JsonPath
                .parse(creada.getResponse().getContentAsString())
                .<Number>read("$.id").longValue();

        /* El dueño recibió la notificación de la pregunta. */
        mockMvc.perform(get("/api/usuario/notificaciones")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(jsonPath("$.contenido[0].tipo").value("PREGUNTA_NUEVA"));

        /* Otro usuario no puede responder (404 sin delatar). */
        Usuario intruso = crearUsuario(ROL_PUBLICADOR);
        mockMvc.perform(post("/api/publicador/preguntas/{id}/respuesta", preguntaId)
                        .with(jwtConRol(ROL_PUBLICADOR, intruso.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\"Hola\"}"))
                .andExpect(status().isNotFound());

        /* El dueño responde y el autor recibe su notificación. */
        mockMvc.perform(post("/api/publicador/preguntas/{id}/respuesta", preguntaId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\"Sí, los sábados a las 10.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Sí, los sábados a las 10."));

        mockMvc.perform(get("/api/usuario/notificaciones")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(jsonPath("$.contenido[0].tipo").value("RESPUESTA_PREGUNTA"));

        /* Pública y respondida: visible para un visitante, sin borrarse. */
        mockMvc.perform(get("/api/actividades/{id}/preguntas", actividad.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].respuesta").value("Sí, los sábados a las 10."));

        mockMvc.perform(delete("/api/usuario/preguntas/{id}", preguntaId)
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportarYOcultarValoracionLaSacaDeLoPublico() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_ADMIN);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        mockMvc.perform(put("/api/usuario/intereses/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"YA_PROBE\"}"))
                .andExpect(status().isOk());

        var valorada = mockMvc.perform(put("/api/usuario/valoraciones/{id}", actividad.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":1,\"comentario\":\"Contenido reportable\"}"))
                .andExpect(status().isOk())
                .andReturn();

        long valoracionId = com.jayway.jsonpath.JsonPath
                .parse(valorada.getResponse().getContentAsString())
                .<Number>read("$.id").longValue();

        /* Reporte del tipo nuevo (el CHECK ampliado del script 29). */
        mockMvc.perform(post("/api/usuario/reportes")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoObjeto\":\"VALORACION\",\"objetoId\":" + valoracionId
                                + ",\"motivo\":\"CONTENIDO_INAPROPIADO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/admin/valoraciones/{id}/ocultar", valoracionId)
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/actividades/{id}/valoraciones", actividad.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0));
    }

    @Test
    void anonimoDevuelve401EnLoPrivado() throws Exception {
        mockMvc.perform(put("/api/usuario/intereses/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"QUIERO_PROBAR\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/usuario/valoraciones/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":5}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/usuario/preguntas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    /* ================= helpers (mismo patron que LikesFotosIT) ================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Confianza IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("confianza-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-confianza-it");
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
        perfil.setNombre("Perfil Confianza IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1201");
        perfil.setWhatsappNormalizado("5492235551201");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para ConfianzaIT."));

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
                        "No hay una ciudad activa con barrio activo para ConfianzaIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Confianza IT");
        ubicacion.setDireccion("Calle confianza 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Confianza IT");
        actividad.setSlug(slugUnico("actividad-confianza-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por ConfianzaIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-1202");
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
                        .subject("confianza-it@dondeentreno.test")
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
