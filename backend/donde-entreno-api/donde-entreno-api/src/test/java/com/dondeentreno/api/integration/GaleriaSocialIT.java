package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.ComentarioImagen;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.ComentarioImagenRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.FotoGuardadaRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.NotificacionRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.ReporteRepository;
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
 * Fase 4 (script 30): comentarios en fotos con moderación por estados,
 * fotos guardadas, toggle de comentarios y sección de galería. La
 * subida directa al storage la cubren los unit tests y el smoke real.
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
class GaleriaSocialIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String ROL_ADMIN = "ADMIN";
    private static final String URL_PUBLICA_BASE =
            "https://proyecto.supabase.co/storage/v1/object/public/imagenes-publicas/galeria-it-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private ComentarioImagenRepository comentarioImagenRepository;

    @Autowired
    private FotoGuardadaRepository fotoGuardadaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private ImagenRepository imagenRepository;

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

    private final List<Long> imagenIds = new ArrayList<>();
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
            comentarioImagenRepository.deleteAll(
                    comentarioImagenRepository.findAll().stream()
                            .filter(comentario -> comentario.getUsuarioId().equals(usuarioId))
                            .toList());
            fotoGuardadaRepository.deleteAll(
                    fotoGuardadaRepository.findAll().stream()
                            .filter(guardada -> guardada.getUsuarioId().equals(usuarioId))
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
        comentarioImagenRepository.flush();
        fotoGuardadaRepository.flush();
        notificacionRepository.flush();
        reporteRepository.flush();

        for (Long imagenId : imagenIds) {
            imagenRepository.findById(imagenId).ifPresent(imagenRepository::delete);
        }
        imagenRepository.flush();

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

        imagenIds.clear();
        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }

    @Test
    void comentarNotificaYSeModeraPorEstados() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Usuario admin = crearUsuario(ROL_ADMIN);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Imagen foto = crearImagenAprobada(actividad);
        Long duenioId = perfil.getUsuario().getId();

        /* Comentar publica DIRECTO y notifica al dueño. */
        var creado = mockMvc.perform(post("/api/usuario/comentarios")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagenId\":\"" + foto.getId()
                                + "\",\"texto\":\"¡Qué lindas instalaciones!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("¡Qué lindas instalaciones!"))
                .andReturn();

        long comentarioId = com.jayway.jsonpath.JsonPath
                .parse(creado.getResponse().getContentAsString())
                .<Number>read("$.id").longValue();

        mockMvc.perform(get("/api/usuario/notificaciones")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(jsonPath("$.contenido[0].tipo").value("COMENTARIO_NUEVO"));

        /* Visible en el GET público SIN sesión. */
        mockMvc.perform(get("/api/imagenes/{id}/comentarios", foto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        /* Reporte del tipo nuevo (CHECK ampliado del script 30). */
        mockMvc.perform(post("/api/usuario/reportes")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoObjeto\":\"COMENTARIO\",\"objetoId\":" + comentarioId
                                + ",\"motivo\":\"SPAM\"}"))
                .andExpect(status().isNoContent());

        /* Un intruso no oculta (404 sin delatar); el dueño sí. */
        mockMvc.perform(patch("/api/publicador/comentarios/{id}/ocultar", comentarioId)
                        .with(jwtConRol(ROL_PUBLICADOR, crearUsuario(ROL_PUBLICADOR).getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/publicador/comentarios/{id}/ocultar", comentarioId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/imagenes/{id}/comentarios", foto.getId()))
                .andExpect(jsonPath("$", hasSize(0)));

        /* El admin también puede ocultar otro comentario. */
        ComentarioImagen otro = new ComentarioImagen();
        otro.setImagenId(foto.getId());
        otro.setUsuarioId(usuario.getId());
        otro.setTexto("Segundo comentario");
        otro.setEstado("VISIBLE");
        otro.setCreatedAt(OffsetDateTime.now());
        long otroId = comentarioImagenRepository.saveAndFlush(otro).getId();

        mockMvc.perform(patch("/api/admin/comentarios/{id}/ocultar", otroId)
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isNoContent());
    }

    @Test
    void toggleDeComentariosYSeccionViajanPorElPatch() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Imagen foto = crearImagenAprobada(actividad);
        Usuario usuario = crearUsuario(ROL_USUARIO);

        mockMvc.perform(patch("/api/publicador/actividades/{aid}/imagenes/{iid}",
                        actividad.getId(), foto.getId())
                        .with(jwtConRol(ROL_PUBLICADOR, perfil.getUsuario().getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seccion\":\"INSTALACIONES\",\"comentariosActivados\":false}"))
                .andExpect(status().isOk());

        /* Con comentarios desactivados, comentar da 400 con mensaje claro. */
        mockMvc.perform(post("/api/usuario/comentarios")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagenId\":\"" + foto.getId() + "\",\"texto\":\"Hola\"}"))
                .andExpect(status().isBadRequest());

        /* La sección viaja en el listado público. */
        mockMvc.perform(get("/api/actividades/{slug}/imagenes", actividad.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seccion").value("INSTALACIONES"));
    }

    @Test
    void guardarFotosEsIdempotenteYElListadoOmiteDespublicadas() throws Exception {
        Usuario usuario = crearUsuario(ROL_USUARIO);
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);
        Imagen foto = crearImagenAprobada(actividad);

        mockMvc.perform(put("/api/usuario/fotos-guardadas/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/usuario/fotos-guardadas/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuario/fotos-guardadas/detalle")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(foto.getId()));

        /* Se despublica la foto: desaparece del listado sin perder el guardado. */
        foto.setActiva(false);
        imagenRepository.saveAndFlush(foto);

        mockMvc.perform(get("/api/usuario/fotos-guardadas/detalle")
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete("/api/usuario/fotos-guardadas/{id}", foto.getId())
                        .with(jwtConRol(ROL_USUARIO, usuario.getId())))
                .andExpect(status().isNoContent());
    }

    @Test
    void anonimoDevuelve401EnLoPrivado() throws Exception {
        mockMvc.perform(post("/api/usuario/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/usuario/fotos-guardadas/{id}", 1L))
                .andExpect(status().isUnauthorized());
        /* Leer comentarios sí es público. */
        mockMvc.perform(get("/api/imagenes/{id}/comentarios", 1L))
                .andExpect(status().isOk());
    }

    /* ================= helpers (mismo patron que LikesFotosIT) ================= */

    private Imagen crearImagenAprobada(Actividad actividad) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl(URL_PUBLICA_BASE + UUID.randomUUID() + ".jpg");
        imagen.setTipoImagen("GALERIA");
        imagen.setOrden(1);
        imagen.setActiva(true);
        imagen.setEstadoModeracion("APROBADA");
        imagen.setComentariosActivados(true);
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.saveAndFlush(imagen);
        imagenIds.add(guardada.getId());
        return guardada;
    }

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Galeria IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("galeria-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-galeria-it");
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
        perfil.setNombre("Perfil Galeria IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1301");
        perfil.setWhatsappNormalizado("5492235551301");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para GaleriaSocialIT."));

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
                        "No hay una ciudad activa con barrio activo para GaleriaSocialIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Galeria IT");
        ubicacion.setDireccion("Calle galeria 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Galeria IT");
        actividad.setSlug(slugUnico("actividad-galeria-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por GaleriaSocialIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-1302");
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
                        .subject("galeria-it@dondeentreno.test")
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
