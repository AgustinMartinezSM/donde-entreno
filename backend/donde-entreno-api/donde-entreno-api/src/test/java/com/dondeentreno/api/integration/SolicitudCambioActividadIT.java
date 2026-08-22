package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SolicitudCambioActividad;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.SolicitudCambioActividadRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de solicitudes de cambio sobre actividades publicadas
 * contra PostgreSQL local: crear (publicador), conflicto por duplicado,
 * revision y aprobacion (admin) aplicando los cambios a la actividad,
 * y rechazo con motivo obligatorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-solicitud-cambio-it-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class SolicitudCambioActividadIT {

    private static final String ESTADO_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_ADMIN = "ADMIN";

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
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private SolicitudCambioActividadRepository solicitudCambioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    @Autowired
    private com.dondeentreno.api.repository.HorarioActividadRepository horarioActividadRepository;

    private final List<Long> solicitudCambioIds = new ArrayList<>();
    private final List<Long> actividadIds = new ArrayList<>();
    private final List<Long> ubicacionIds = new ArrayList<>();
    private final List<Long> perfilPublicadorIds = new ArrayList<>();
    private final List<Long> usuarioIds = new ArrayList<>();
    private final List<String> marcadores = new ArrayList<>();

    @BeforeEach
    void verificarDatasourceLocal() {
        verificarVariablesLocalesPresentes();

        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @AfterEach
    void limpiarDatosCreadosPorElTest() {
        verificarDatasourceLocal();
        registrarResiduosPorMarcador();

        for (Long solicitudId : solicitudCambioIds) {
            solicitudCambioRepository.findById(solicitudId).ifPresent(solicitudCambioRepository::delete);
        }
        solicitudCambioRepository.flush();

        /*
          Los horarios no cascadean desde la actividad: se borran antes
          para que la FK no bloquee la limpieza.
        */
        horarioActividadRepository.findAll().stream()
                .filter(horario -> horario.getActividad() != null
                        && actividadIds.contains(horario.getActividad().getId()))
                .forEach(horarioActividadRepository::delete);
        horarioActividadRepository.flush();

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

        for (String marcador : marcadores) {
            assertFalse(existeResiduoConMarcador(marcador), "Quedaron datos residuales del test: " + marcador);
        }

        solicitudCambioIds.clear();
        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
        marcadores.clear();
    }

    @Test
    void flujoCompletoCrearConflictoYAprobarAplicaCambios() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Usuario admin = crearAdmin(marcador);
        Actividad actividad = crearActividadPublicada(marcador, publicador.perfil(), referencias);

        String tituloPropuesto = "Titulo nuevo " + marcador;

        // 1) El publicador crea la solicitud de cambio.
        ResultActions creacion = mockMvc.perform(
                        post("/api/publicador/actividades/" + actividad.getId() + "/solicitudes-cambio")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CuerpoCambio(
                                        tituloPropuesto,
                                        new BigDecimal("22222.00")
                                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.actividadId").value(actividad.getId()))
                .andExpect(jsonPath("$.cambios.length()").value(2))
                .andExpect(jsonPath("$.cambios[0].campo").value("titulo"))
                .andExpect(jsonPath("$.cambios[0].valorActual").value(actividad.getTitulo()))
                .andExpect(jsonPath("$.cambios[0].valorPropuesto").value(tituloPropuesto));

        long solicitudId = leerJson(creacion).path("id").asLong();
        solicitudCambioIds.add(solicitudId);

        // 2) Un segundo intento con la solicitud abierta devuelve 409.
        mockMvc.perform(
                        post("/api/publicador/actividades/" + actividad.getId() + "/solicitudes-cambio")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"descripcion\":\"Otro cambio " + marcador + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 3) El publicador la ve en su listado.
        mockMvc.perform(get("/api/publicador/solicitudes-cambio")
                        .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(solicitudId))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"));

        // 4) El admin ve el detalle con la comparacion.
        mockMvc.perform(get("/api/admin/solicitudes-cambio/" + solicitudId)
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilPublicadorId").value(publicador.perfil().getId()))
                .andExpect(jsonPath("$.cambios[0].valorActual").value(actividad.getTitulo()));

        // 5) El admin aprueba: los cambios se aplican a la actividad.
        mockMvc.perform(post("/api/admin/solicitudes-cambio/" + solicitudId + "/aprobar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        Actividad actividadActualizada = actividadRepository.findById(actividad.getId()).orElseThrow();
        assertEquals(tituloPropuesto, actividadActualizada.getTitulo());
        assertEquals(0, new BigDecimal("22222.00").compareTo(actividadActualizada.getPrecioReferencia()));

        SolicitudCambioActividad solicitudResuelta =
                solicitudCambioRepository.findById(solicitudId).orElseThrow();
        assertEquals("APROBADA", solicitudResuelta.getEstado());
        assertNotNull(solicitudResuelta.getResueltoAt());
        assertEquals(admin.getId(), solicitudResuelta.getResueltoPor().getId());

        // 6) Resuelta la anterior, se puede abrir una nueva.
        ResultActions segunda = mockMvc.perform(
                        post("/api/publicador/actividades/" + actividad.getId() + "/solicitudes-cambio")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"descripcion\":\"Descripcion nueva " + marcador + "\"}"))
                .andExpect(status().isCreated());
        solicitudCambioIds.add(leerJson(segunda).path("id").asLong());
    }

    @Test
    void aprobarSolicitudCompletaAplicaDeporteEdadesEnfoqueUbicacionYHorarios() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Usuario admin = crearAdmin(marcador);
        Actividad actividad = crearActividadPublicada(marcador, publicador.perfil(), referencias);

        Deporte deportePropuesto = deporteRepository.findByActivoTrue().stream()
                .filter(deporte -> !deporte.getId().equals(referencias.deporte().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Se necesitan al menos dos deportes activos para el flujo completo."
                ));

        // Horario vigente que la aprobacion debe desactivar.
        OffsetDateTime ahora = OffsetDateTime.now();
        com.dondeentreno.api.entity.HorarioActividad vigente =
                new com.dondeentreno.api.entity.HorarioActividad();
        vigente.setActividad(actividad);
        vigente.setDiaSemana("LUNES");
        vigente.setHoraInicio(java.time.LocalTime.of(9, 0));
        vigente.setHoraFin(java.time.LocalTime.of(10, 0));
        vigente.setActivo(true);
        vigente.setCreatedAt(ahora);
        vigente.setUpdatedAt(ahora);
        long vigenteId = horarioActividadRepository.saveAndFlush(vigente).getId();

        var cuerpo = new java.util.LinkedHashMap<String, Object>();
        cuerpo.put("deporteId", deportePropuesto.getId());
        cuerpo.put("edadMinima", 21);
        cuerpo.put("edadMaxima", 70);
        cuerpo.put("enfoque", "COMPETITIVO");
        cuerpo.put("ubicacionNombre", "Sede renovada " + marcador);
        cuerpo.put("ubicacionDireccion", "Calle aprobada " + marcador);
        cuerpo.put("ubicacionReferencia", "Referencia nueva " + marcador);
        cuerpo.put("ubicacionBarrioId", referencias.barrio().getId());
        cuerpo.put("cambiaHorarios", true);
        cuerpo.put("horarios", List.of(java.util.Map.of(
                "diaSemana", "MARTES",
                "horaInicio", "10:00",
                "horaFin", "11:30",
                "observacion", "Trae ropa comoda"
        )));

        ResultActions creacion = mockMvc.perform(
                        post("/api/publicador/actividades/" + actividad.getId() + "/solicitudes-cambio")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cambios.length()").value(6))
                .andExpect(jsonPath("$.cambios[0].campo").value("deporte"))
                .andExpect(jsonPath("$.cambios[0].valorPropuesto").value(deportePropuesto.getNombre()))
                .andExpect(jsonPath("$.cambios[4].campo").value("ubicacion"))
                .andExpect(jsonPath("$.cambios[5].campo").value("horarios"));

        long solicitudId = leerJson(creacion).path("id").asLong();
        solicitudCambioIds.add(solicitudId);

        mockMvc.perform(post("/api/admin/solicitudes-cambio/" + solicitudId + "/aprobar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        Actividad actualizada = actividadRepository.findById(actividad.getId()).orElseThrow();
        assertEquals(deportePropuesto.getId(), actualizada.getDeporte().getId());
        assertEquals(21, actualizada.getEdadMinima());
        assertEquals(70, actualizada.getEdadMaxima());
        assertEquals("COMPETITIVO", actualizada.getEnfoque());

        /*
          La sede era exclusiva de esta actividad: se edita EN EL LUGAR
          (mismo id), no se crea una nueva. Se recarga por repo porque
          la relacion es LAZY y aca no hay sesion abierta.
        */
        assertEquals(actividad.getUbicacion().getId(), actualizada.getUbicacion().getId());
        Ubicacion ubicacionActualizada =
                ubicacionRepository.findById(actualizada.getUbicacion().getId()).orElseThrow();
        assertEquals("Sede renovada " + marcador, ubicacionActualizada.getNombre());
        assertEquals("Calle aprobada " + marcador, ubicacionActualizada.getDireccion());

        // Horarios: el vigente quedo inactivo y el propuesto es el unico activo.
        com.dondeentreno.api.entity.HorarioActividad vigenteRefrescado =
                horarioActividadRepository.findById(vigenteId).orElseThrow();
        assertEquals(Boolean.FALSE, vigenteRefrescado.getActivo());

        List<com.dondeentreno.api.entity.HorarioActividad> activos =
                horarioActividadRepository
                        .findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(actividad.getId());
        assertEquals(1, activos.size());
        assertEquals("MARTES", activos.get(0).getDiaSemana());
        assertEquals(java.time.LocalTime.of(10, 0), activos.get(0).getHoraInicio());
        assertEquals(java.time.LocalTime.of(11, 30), activos.get(0).getHoraFin());
        assertEquals("Trae ropa comoda", activos.get(0).getObservacion());
    }

    @Test
    void rechazarRequiereMotivoYNoTocaLaActividad() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Usuario admin = crearAdmin(marcador);
        Actividad actividad = crearActividadPublicada(marcador, publicador.perfil(), referencias);
        String tituloOriginal = actividad.getTitulo();

        ResultActions creacion = mockMvc.perform(
                        post("/api/publicador/actividades/" + actividad.getId() + "/solicitudes-cambio")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"titulo\":\"Titulo rechazable " + marcador + "\"}"))
                .andExpect(status().isCreated());
        long solicitudId = leerJson(creacion).path("id").asLong();
        solicitudCambioIds.add(solicitudId);

        // Rechazo sin motivo: 400.
        mockMvc.perform(patch("/api/admin/solicitudes-cambio/" + solicitudId + "/estado")
                        .with(jwtConRol(ROL_ADMIN, admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RECHAZADA\"}"))
                .andExpect(status().isBadRequest());

        // Rechazo con motivo: 200 y la actividad no cambia.
        mockMvc.perform(patch("/api/admin/solicitudes-cambio/" + solicitudId + "/estado")
                        .with(jwtConRol(ROL_ADMIN, admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RECHAZADA\",\"motivoRechazo\":\"No corresponde el cambio.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("No corresponde el cambio."));

        Actividad actividadIntacta = actividadRepository.findById(actividad.getId()).orElseThrow();
        assertEquals(tituloOriginal, actividadIntacta.getTitulo());
    }

    @Test
    void endpointsDeCambioRespetanRoles() throws Exception {
        mockMvc.perform(get("/api/publicador/solicitudes-cambio"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/publicador/solicitudes-cambio")
                        .with(jwtConRol(ROL_USUARIO, Long.MAX_VALUE - 11)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/solicitudes-cambio"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/solicitudes-cambio")
                        .with(jwtConRol(ROL_PUBLICADOR, Long.MAX_VALUE - 12)))
                .andExpect(status().isForbidden());
    }

    private Publicador crearPublicador(String marcador, Ciudad ciudad) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_PUBLICADOR)
                .orElseThrow(() -> new IllegalStateException("No existe el rol PUBLICADOR para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Publicador " + marcador);
        usuario.setApellido("Cambio IT");
        usuario.setEmail("solicitud-cambio-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-solicitud-cambio-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(usuarioGuardado.getId());

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(usuarioGuardado);
        perfil.setNombre("Perfil " + marcador);
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(usuarioGuardado.getEmail());
        perfil.setWhatsapp("+54 9 223 555-0201");
        perfil.setWhatsappNormalizado("5492235550201");
        perfil.setTelefonoContacto("+54 9 223 555-0202");
        perfil.setTelefonoContactoNormalizado("5492235550202");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador perfilGuardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(perfilGuardado.getId());

        return new Publicador(usuarioGuardado, perfilGuardado);
    }

    private Usuario crearAdmin(String marcador) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_ADMIN)
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMIN para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Admin " + marcador);
        usuario.setApellido("Cambio IT");
        usuario.setEmail("admin-cambio-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-admin-cambio-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private Actividad crearActividadPublicada(
            String marcador,
            PerfilPublicador perfil,
            Referencias referencias
    ) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede " + marcador);
        ubicacion.setDireccion("Calle cambio " + marcador);
        ubicacion.setReferencia("Referencia " + marcador);
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad " + marcador);
        actividad.setSlug(slugUnico("actividad-cambio-" + marcador));
        actividad.setDescripcion("Actividad creada por SolicitudCambioActividadIT " + marcador);
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0203");
        actividad.setInstagramContacto("@solicitud_cambio_it");
        actividad.setEmailContacto(perfil.getEmailContacto());
        actividad.setEstadoPublicacion(ESTADO_PUBLICADA);
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad actividadGuardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(actividadGuardada.getId());
        return actividadGuardada;
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para SolicitudCambioActividadIT."));

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
                        "No hay una ciudad activa con barrio activo para SolicitudCambioActividadIT."
                ));
    }

    private JsonNode leerJson(ResultActions resultActions) throws Exception {
        String response = resultActions.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("solicitud-cambio-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private void registrarResiduosPorMarcador() {
        for (String marcador : marcadores) {
            solicitudCambioRepository.findAll().stream()
                    .filter(solicitud -> contieneMarcador(solicitud.getTitulo(), marcador)
                            || contieneMarcador(solicitud.getDescripcion(), marcador))
                    .map(SolicitudCambioActividad::getId)
                    .filter(id -> !solicitudCambioIds.contains(id))
                    .forEach(solicitudCambioIds::add);

            actividadRepository.findAll().stream()
                    .filter(actividad -> contieneMarcador(actividad.getTitulo(), marcador)
                            || contieneMarcador(actividad.getSlug(), marcador))
                    .map(Actividad::getId)
                    .filter(id -> !actividadIds.contains(id))
                    .forEach(actividadIds::add);

            ubicacionRepository.findAll().stream()
                    .filter(ubicacion -> contieneMarcador(ubicacion.getNombre(), marcador)
                            || contieneMarcador(ubicacion.getDireccion(), marcador)
                            || contieneMarcador(ubicacion.getReferencia(), marcador))
                    .map(Ubicacion::getId)
                    .filter(id -> !ubicacionIds.contains(id))
                    .forEach(ubicacionIds::add);

            perfilPublicadorRepository.findAll().stream()
                    .filter(perfil -> contieneMarcador(perfil.getNombre(), marcador)
                            || contieneMarcador(perfil.getEmailContacto(), marcador))
                    .map(PerfilPublicador::getId)
                    .filter(id -> !perfilPublicadorIds.contains(id))
                    .forEach(perfilPublicadorIds::add);

            usuarioRepository.findAll().stream()
                    .filter(usuario -> contieneMarcador(usuario.getNombre(), marcador)
                            || contieneMarcador(usuario.getEmail(), marcador))
                    .map(Usuario::getId)
                    .filter(id -> !usuarioIds.contains(id))
                    .forEach(usuarioIds::add);
        }
    }

    private boolean existeResiduoConMarcador(String marcador) {
        return solicitudCambioRepository.findAll().stream()
                .anyMatch(solicitud -> contieneMarcador(solicitud.getTitulo(), marcador)
                        || contieneMarcador(solicitud.getDescripcion(), marcador))
                || actividadRepository.findAll().stream()
                        .anyMatch(actividad -> contieneMarcador(actividad.getTitulo(), marcador)
                                || contieneMarcador(actividad.getSlug(), marcador))
                || ubicacionRepository.findAll().stream()
                        .anyMatch(ubicacion -> contieneMarcador(ubicacion.getNombre(), marcador)
                                || contieneMarcador(ubicacion.getDireccion(), marcador)
                                || contieneMarcador(ubicacion.getReferencia(), marcador))
                || perfilPublicadorRepository.findAll().stream()
                        .anyMatch(perfil -> contieneMarcador(perfil.getNombre(), marcador)
                                || contieneMarcador(perfil.getEmailContacto(), marcador))
                || usuarioRepository.findAll().stream()
                        .anyMatch(usuario -> contieneMarcador(usuario.getNombre(), marcador)
                                || contieneMarcador(usuario.getEmail(), marcador));
    }

    private boolean contieneMarcador(String texto, String marcador) {
        return texto != null && texto.contains(marcador);
    }

    private String marcadorUnico() {
        String marcador = "IT-SOLICITUD-CAMBIO-" + UUID.randomUUID();
        marcadores.add(marcador);
        return marcador;
    }

    private String slugUnico(String base) {
        return base.toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-");
    }

    private void verificarVariablesLocalesPresentes() {
        assertFalse(
                System.getenv("DONDEENTRENO_TEST_DB_USERNAME") == null
                        || System.getenv("DONDEENTRENO_TEST_DB_USERNAME").isBlank(),
                "Debe existir DONDEENTRENO_TEST_DB_USERNAME para integration-local."
        );
        assertFalse(
                System.getenv("DONDEENTRENO_TEST_DB_PASSWORD") == null
                        || System.getenv("DONDEENTRENO_TEST_DB_PASSWORD").isBlank(),
                "Debe existir DONDEENTRENO_TEST_DB_PASSWORD para integration-local."
        );
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        return url.toLowerCase(Locale.ROOT).matches(
                ".*(supabase|render|amazonaws|azure|neon|railway|aiven|digitalocean|\\.com|\\.net|\\.io|\\.app).*"
        );
    }

    private record Publicador(Usuario usuario, PerfilPublicador perfil) {
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }

    /**
     * Cuerpo tipado del POST de creacion (solo campos usados por el test).
     */
    private record CuerpoCambio(String titulo, BigDecimal precioReferencia) {
    }
}
