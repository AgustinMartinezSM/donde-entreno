package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.HorarioActividad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.SolicitudPublicacion;
import com.dondeentreno.api.entity.SolicitudPublicacionHorario;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.HorarioActividadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionHorarioRepository;
import com.dondeentreno.api.repository.SolicitudPublicacionRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
class SolicitudPublicacionAprobacionIT {

    private static final String ESTADO_APROBADA = "APROBADA";
    private static final String ESTADO_EN_REVISION = "EN_REVISION";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_PUBLICACION_PUBLICADA = "PUBLICADA";
    private static final String TIPO_PUBLICADOR = "ESCUELA_DEPORTIVA";
    private static final String ROL_SUPER_ADMIN = "SUPER_ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private SolicitudPublicacionRepository solicitudPublicacionRepository;

    @Autowired
    private SolicitudPublicacionHorarioRepository solicitudPublicacionHorarioRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private HorarioActividadRepository horarioActividadRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    private final List<Long> solicitudIds = new ArrayList<>();
    private final List<Long> solicitudHorarioIds = new ArrayList<>();
    private final List<Long> actividadIds = new ArrayList<>();
    private final List<Long> perfilPublicadorIds = new ArrayList<>();
    private final List<Long> ubicacionIds = new ArrayList<>();
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

        for (Long solicitudId : solicitudIds) {
            solicitudPublicacionRepository.findById(solicitudId).ifPresent(solicitud -> {
                solicitud.setActividadGenerada(null);
                solicitudPublicacionRepository.saveAndFlush(solicitud);
            });
        }

        for (Long actividadId : actividadIds) {
            List<HorarioActividad> horarios =
                    horarioActividadRepository.findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(actividadId);
            horarioActividadRepository.deleteAll(horarios);
            horarioActividadRepository.flush();
        }

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

        for (Long solicitudHorarioId : solicitudHorarioIds) {
            solicitudPublicacionHorarioRepository.findById(solicitudHorarioId)
                    .ifPresent(solicitudPublicacionHorarioRepository::delete);
        }
        solicitudPublicacionHorarioRepository.flush();

        for (Long solicitudId : solicitudIds) {
            solicitudPublicacionRepository.findById(solicitudId).ifPresent(solicitudPublicacionRepository::delete);
        }
        solicitudPublicacionRepository.flush();

        for (Long usuarioId : usuarioIds) {
            usuarioRepository.findById(usuarioId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();

        for (String marcador : marcadores) {
            assertFalse(existeResiduoConMarcador(marcador), "Quedaron datos residuales del test: " + marcador);
        }

        solicitudIds.clear();
        solicitudHorarioIds.clear();
        actividadIds.clear();
        perfilPublicadorIds.clear();
        ubicacionIds.clear();
        usuarioIds.clear();
        marcadores.clear();
    }

    @Test
    void aprobarSolicitudPendienteValidaCreaActividadHorariosYVinculaSolicitud() throws Exception {
        DatosBase datos = crearDatosBase(ESTADO_PENDIENTE);

        JsonNode response = aprobarYLeerJson(datos.solicitud().getId(), datos.admin());

        Long actividadId = response.get("actividadId").asLong();
        actividadIds.add(actividadId);
        registrarPerfilYUbicacionCreados(datos, actividadId);

        assertEquals(datos.solicitud().getId(), response.get("solicitudId").asLong());
        assertEquals(ESTADO_APROBADA, response.get("estado").asText());
        assertFalse(response.get("actividadSlug").asText().isBlank());
        assertEquals(datos.solicitud().getNombreActividad(), response.get("actividadTitulo").asText());
        assertFalse(response.get("mensaje").asText().isBlank());

        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(datos.solicitud().getId()).orElseThrow();
        Actividad actividad = actividadRepository.findById(actividadId).orElseThrow();
        List<HorarioActividad> horarios =
                horarioActividadRepository.findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(actividadId);

        assertEquals(ESTADO_APROBADA, solicitud.getEstado());
        assertNotNull(solicitud.getActividadGenerada());
        assertEquals(actividadId, solicitud.getActividadGenerada().getId());
        assertNotNull(solicitud.getRevisadoPorUsuario());
        assertEquals(datos.admin().getId(), solicitud.getRevisadoPorUsuario().getId());
        assertNotNull(solicitud.getRevisionFinalizadaAt());
        assertNull(solicitud.getMotivoRechazo());
        assertEquals(Boolean.TRUE, actividad.getActiva());
        assertEquals(ESTADO_PUBLICACION_PUBLICADA, actividad.getEstadoPublicacion());
        assertFalse(horarios.isEmpty());
        assertEquals("LUNES", horarios.get(0).getDiaSemana());
    }

    @Test
    void aprobarSolicitudEnRevisionValidaApruebaCorrectamente() throws Exception {
        DatosBase datos = crearDatosBase(ESTADO_EN_REVISION);

        JsonNode response = aprobarYLeerJson(datos.solicitud().getId(), datos.admin());

        Long actividadId = response.get("actividadId").asLong();
        actividadIds.add(actividadId);
        registrarPerfilYUbicacionCreados(datos, actividadId);

        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(datos.solicitud().getId()).orElseThrow();
        List<HorarioActividad> horarios =
                horarioActividadRepository.findByActivoTrueAndActividad_IdOrderByDiaSemanaAscHoraInicioAsc(actividadId);

        assertEquals(ESTADO_APROBADA, response.get("estado").asText());
        assertEquals(ESTADO_APROBADA, solicitud.getEstado());
        assertNotNull(solicitud.getActividadGenerada());
        assertEquals(datos.admin().getId(), solicitud.getRevisadoPorUsuario().getId());
        assertNotNull(solicitud.getRevisionFinalizadaAt());
        assertFalse(horarios.isEmpty());
    }

    @Test
    void aprobarSolicitudRechazadaDevuelveBadRequestYNoCreaActividad() throws Exception {
        DatosBase datos = crearDatosBase(ESTADO_RECHAZADA);

        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", datos.solicitud().getId())
                        .with(jwtConRol(ROL_SUPER_ADMIN, datos.admin().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertSolicitudSinActividad(datos.solicitud().getId());
        assertNoExisteActividadConMarcador(datos.marcador());
    }

    @Test
    void aprobarSolicitudYaAprobadaDevuelveBadRequestYNoCreaActividad() throws Exception {
        DatosBase datos = crearDatosBase(ESTADO_APROBADA);

        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", datos.solicitud().getId())
                        .with(jwtConRol(ROL_SUPER_ADMIN, datos.admin().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertSolicitudSinActividad(datos.solicitud().getId());
        assertNoExisteActividadConMarcador(datos.marcador());
    }

    @Test
    void aprobarSolicitudConActividadGeneradaExistenteDevuelveBadRequestYNoCreaOtraActividad() throws Exception {
        DatosBase datos = crearDatosBase(ESTADO_PENDIENTE);
        PerfilPublicador perfil = crearPerfilPublicador(datos);
        Ubicacion ubicacion = crearUbicacion(datos, perfil);
        Actividad actividadExistente = crearActividadExistente(datos, perfil, ubicacion);

        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(datos.solicitud().getId()).orElseThrow();
        solicitud.setActividadGenerada(actividadExistente);
        solicitudPublicacionRepository.saveAndFlush(solicitud);

        long actividadesAntes = actividadRepository.findAll().stream()
                .filter(actividad -> contieneMarcador(actividad.getTitulo(), datos.marcador()))
                .count();

        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", datos.solicitud().getId())
                        .with(jwtConRol(ROL_SUPER_ADMIN, datos.admin().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        long actividadesDespues = actividadRepository.findAll().stream()
                .filter(actividad -> contieneMarcador(actividad.getTitulo(), datos.marcador()))
                .count();

        assertEquals(actividadesAntes, actividadesDespues);
    }

    @Test
    void aprobarSolicitudInexistenteDevuelveNotFound() throws Exception {
        Usuario admin = crearAdmin(marcadorUnico());

        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", Long.MAX_VALUE)
                        .with(jwtConRol(ROL_SUPER_ADMIN, admin.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void aprobarSolicitudSinTokenDevuelveUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", Long.MAX_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void aprobarSolicitudConRolUsuarioDevuelveForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", Long.MAX_VALUE)
                        .with(jwtConRol("USUARIO", 123456789L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void aprobarSolicitudSinHorariosDevuelveBadRequestYNoCreaActividad() throws Exception {
        DatosBase datos = crearDatosBaseSinHorarios(ESTADO_PENDIENTE);

        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", datos.solicitud().getId())
                        .with(jwtConRol(ROL_SUPER_ADMIN, datos.admin().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertSolicitudSinActividad(datos.solicitud().getId());
        assertNoExisteActividadConMarcador(datos.marcador());
    }

    @Test
    void aprobarSolicitudConDeporteOtroSinReferenciaRealDevuelveBadRequestYNoCreaActividad() throws Exception {
        DatosBase datos = crearDatosBase(ESTADO_PENDIENTE);
        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(datos.solicitud().getId()).orElseThrow();
        solicitud.setDeporte(null);
        solicitud.setDeporteOtro("Deporte otro " + datos.marcador());
        solicitudPublicacionRepository.saveAndFlush(solicitud);

        mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", datos.solicitud().getId())
                        .with(jwtConRol(ROL_SUPER_ADMIN, datos.admin().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertSolicitudSinActividad(datos.solicitud().getId());
        assertNoExisteActividadConMarcador(datos.marcador());
    }

    private JsonNode aprobarYLeerJson(Long solicitudId, Usuario admin) throws Exception {
        String response = mockMvc.perform(post("/api/admin/solicitudes-publicacion/{id}/aprobar", solicitudId)
                        .with(jwtConRol(ROL_SUPER_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudId").value(solicitudId))
                .andExpect(jsonPath("$.estado").value(ESTADO_APROBADA))
                .andExpect(jsonPath("$.actividadId", notNullValue()))
                .andExpect(jsonPath("$.actividadSlug", not("")))
                .andExpect(jsonPath("$.actividadTitulo", not("")))
                .andExpect(jsonPath("$.mensaje", not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private DatosBase crearDatosBase(String estado) {
        return crearDatosBase(estado, true);
    }

    private DatosBase crearDatosBaseSinHorarios(String estado) {
        return crearDatosBase(estado, false);
    }

    private DatosBase crearDatosBase(String estado, boolean conHorario) {
        verificarDatasourceLocal();

        String marcador = marcadorUnico();
        Referencias referencias = obtenerReferenciasActivas();
        Usuario admin = crearAdmin(marcador);
        SolicitudPublicacion solicitud = crearSolicitud(marcador, estado, referencias);

        if (conHorario) {
            crearHorarioSolicitud(solicitud);
        }

        return new DatosBase(marcador, admin, solicitud, referencias);
    }

    private Usuario crearAdmin(String marcador) {
        verificarDatasourceLocal();

        Rol rol = rolRepository.findByNombre(ROL_SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("No existe el rol SUPER_ADMIN para integration-local."));
        OffsetDateTime ahora = OffsetDateTime.now();

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Admin " + marcador);
        usuario.setApellido("Aprobacion IT");
        usuario.setEmail("aprobacion-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-aprobacion-it");
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private SolicitudPublicacion crearSolicitud(String marcador, String estado, Referencias referencias) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();

        SolicitudPublicacion solicitud = new SolicitudPublicacion();
        solicitud.setCodigoSeguimiento(codigoSeguimientoUnico());
        solicitud.setOrigen("FORMULARIO_WEB");
        solicitud.setEstado(estado);
        solicitud.setTipoPublicador(TIPO_PUBLICADOR);
        solicitud.setNombrePublicador("Escuela " + marcador);
        solicitud.setNombreActividad("Actividad " + marcador);
        solicitud.setDeporte(referencias.deporte());
        solicitud.setDescripcion("Solicitud de aprobacion integration-local " + marcador);
        solicitud.setNivel("PRINCIPIANTE");
        solicitud.setEnfoque("RECREATIVO");
        solicitud.setModalidad("PRESENCIAL");
        solicitud.setEdadMinima(18);
        solicitud.setEdadMaxima(60);
        solicitud.setPrecioReferencia(new BigDecimal("15000.00"));
        solicitud.setMostrarPrecio(true);
        solicitud.setCiudad(referencias.ciudad());
        solicitud.setBarrio(referencias.barrio());
        solicitud.setNombreLugar("Sede " + marcador);
        solicitud.setDireccion("Calle Integracion " + marcador);
        solicitud.setReferenciaUbicacion("Referencia " + marcador);
        solicitud.setWhatsapp("+54 9 223 555-0000");
        solicitud.setWhatsappNormalizado("5492235550000");
        solicitud.setInstagram("@it_aprobacion");
        solicitud.setEmail("aprobacion-it-" + UUID.randomUUID() + "@dondeentreno.test");
        solicitud.setObservacionesSolicitante("Observacion " + marcador);
        solicitud.setAceptaCondiciones(true);
        if (ESTADO_RECHAZADA.equals(estado)) {
            solicitud.setMotivoRechazo("Rechazo de prueba " + marcador);
            solicitud.setRevisionIniciadaAt(ahora);
            solicitud.setRevisionFinalizadaAt(ahora);
        }
        solicitud.setCreatedAt(ahora);
        solicitud.setUpdatedAt(ahora);

        SolicitudPublicacion guardada = solicitudPublicacionRepository.saveAndFlush(solicitud);
        solicitudIds.add(guardada.getId());
        return guardada;
    }

    private SolicitudPublicacionHorario crearHorarioSolicitud(SolicitudPublicacion solicitud) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();

        SolicitudPublicacionHorario horario = new SolicitudPublicacionHorario();
        horario.setSolicitudPublicacion(solicitud);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 30));
        horario.setObservacion("Horario integration-local");
        horario.setCreatedAt(ahora);
        horario.setUpdatedAt(ahora);

        SolicitudPublicacionHorario guardado = solicitudPublicacionHorarioRepository.saveAndFlush(horario);
        solicitudHorarioIds.add(guardado.getId());
        return guardado;
    }

    private PerfilPublicador crearPerfilPublicador(DatosBase datos) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(datos.admin());
        perfil.setNombre("Perfil existente " + datos.marcador());
        perfil.setTipoPublicador(TIPO_PUBLICADOR);
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador guardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(guardado.getId());
        return guardado;
    }

    private Ubicacion crearUbicacion(DatosBase datos, PerfilPublicador perfil) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(datos.referencias().ciudad());
        ubicacion.setBarrio(datos.referencias().barrio());
        ubicacion.setNombre("Ubicacion existente " + datos.marcador());
        ubicacion.setDireccion("Direccion existente " + datos.marcador());
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion guardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(guardada.getId());
        return guardada;
    }

    private Actividad crearActividadExistente(DatosBase datos, PerfilPublicador perfil, Ubicacion ubicacion) {
        verificarDatasourceLocal();

        OffsetDateTime ahora = OffsetDateTime.now();
        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(datos.referencias().deporte());
        actividad.setUbicacion(ubicacion);
        actividad.setTitulo("Actividad existente " + datos.marcador());
        actividad.setSlug("actividad-existente-" + UUID.randomUUID());
        actividad.setDescripcion("Actividad vinculada por integration-local.");
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setMostrarPrecio(false);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setEstadoPublicacion(ESTADO_PUBLICACION_PUBLICADA);
        actividad.setActiva(true);
        actividad.setCreatedAt(ahora);
        actividad.setUpdatedAt(ahora);

        Actividad guardada = actividadRepository.saveAndFlush(actividad);
        actividadIds.add(guardada.getId());
        return guardada;
    }

    private void registrarPerfilYUbicacionCreados(DatosBase datos, Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId).orElseThrow();

        perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        datos.admin().getId(),
                        TIPO_PUBLICADOR,
                        datos.solicitud().getNombrePublicador()
                )
                .map(PerfilPublicador::getId)
                .filter(id -> !perfilPublicadorIds.contains(id))
                .ifPresent(perfilPublicadorIds::add);

        perfilPublicadorRepository
                .findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
                        datos.admin().getId(),
                        TIPO_PUBLICADOR,
                        datos.solicitud().getNombrePublicador()
                )
                .flatMap(perfil -> ubicacionRepository
                        .findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
                                perfil.getId(),
                                datos.referencias().ciudad().getId(),
                                datos.referencias().barrio().getId(),
                                datos.solicitud().getNombreLugar(),
                                datos.solicitud().getDireccion()
                        ))
                .map(Ubicacion::getId)
                .filter(id -> !ubicacionIds.contains(id))
                .ifPresent(ubicacionIds::add);

        assertEquals(datos.solicitud().getNombreActividad(), actividad.getTitulo());
    }

    private void registrarResiduosPorMarcador() {
        for (String marcador : marcadores) {
            solicitudPublicacionRepository.findAll().stream()
                    .filter(solicitud -> contieneMarcador(solicitud.getNombreActividad(), marcador)
                            || contieneMarcador(solicitud.getNombrePublicador(), marcador))
                    .map(SolicitudPublicacion::getId)
                    .filter(id -> !solicitudIds.contains(id))
                    .forEach(solicitudIds::add);

            actividadRepository.findAll().stream()
                    .filter(actividad -> contieneMarcador(actividad.getTitulo(), marcador))
                    .map(Actividad::getId)
                    .filter(id -> !actividadIds.contains(id))
                    .forEach(actividadIds::add);

            ubicacionRepository.findAll().stream()
                    .filter(ubicacion -> contieneMarcador(ubicacion.getNombre(), marcador)
                            || contieneMarcador(ubicacion.getDireccion(), marcador))
                    .map(Ubicacion::getId)
                    .filter(id -> !ubicacionIds.contains(id))
                    .forEach(ubicacionIds::add);

            perfilPublicadorRepository.findAll().stream()
                    .filter(perfil -> contieneMarcador(perfil.getNombre(), marcador))
                    .map(PerfilPublicador::getId)
                    .filter(id -> !perfilPublicadorIds.contains(id))
                    .forEach(perfilPublicadorIds::add);

            usuarioRepository.findAll().stream()
                    .filter(usuario -> contieneMarcador(usuario.getNombre(), marcador))
                    .map(Usuario::getId)
                    .filter(id -> !usuarioIds.contains(id))
                    .forEach(usuarioIds::add);
        }
    }

    private void assertSolicitudSinActividad(Long solicitudId) {
        SolicitudPublicacion solicitud = solicitudPublicacionRepository.findById(solicitudId).orElseThrow();
        assertNull(solicitud.getActividadGenerada());
    }

    private void assertNoExisteActividadConMarcador(String marcador) {
        assertFalse(actividadRepository.findAll().stream()
                .anyMatch(actividad -> contieneMarcador(actividad.getTitulo(), marcador)));
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para el test de aprobacion."));

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
                        "No hay una ciudad activa con barrio activo para el test de aprobacion."
                ));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("aprobacion-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private boolean existeResiduoConMarcador(String marcador) {
        return solicitudPublicacionRepository.findAll().stream()
                .anyMatch(solicitud -> contieneMarcador(solicitud.getNombreActividad(), marcador)
                        || contieneMarcador(solicitud.getNombrePublicador(), marcador))
                || actividadRepository.findAll().stream()
                        .anyMatch(actividad -> contieneMarcador(actividad.getTitulo(), marcador))
                || perfilPublicadorRepository.findAll().stream()
                        .anyMatch(perfil -> contieneMarcador(perfil.getNombre(), marcador))
                || ubicacionRepository.findAll().stream()
                        .anyMatch(ubicacion -> contieneMarcador(ubicacion.getNombre(), marcador)
                                || contieneMarcador(ubicacion.getDireccion(), marcador))
                || usuarioRepository.findAll().stream()
                        .anyMatch(usuario -> contieneMarcador(usuario.getNombre(), marcador));
    }

    private boolean contieneMarcador(String texto, String marcador) {
        return texto != null && texto.contains(marcador);
    }

    private String marcadorUnico() {
        String marcador = "IT-APROBACION-" + UUID.randomUUID();
        marcadores.add(marcador);
        return marcador;
    }

    private String codigoSeguimientoUnico() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return "DEP-20260701-" + uuid.substring(0, 8);
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

    private record DatosBase(
            String marcador,
            Usuario admin,
            SolicitudPublicacion solicitud,
            Referencias referencias
    ) {
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }
}
