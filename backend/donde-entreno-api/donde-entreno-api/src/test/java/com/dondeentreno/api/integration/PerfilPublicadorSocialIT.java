package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.EventoInteraccion;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.PreguntaActividad;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.entity.Valoracion;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.EventoInteraccionRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.PreguntaActividadRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 5 (script 31): stats de cabecera, opiniones y preguntas
 * agregadas por publicador, destacadas y tracking del perfil.
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
class PerfilPublicadorSocialIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String URL_PUBLICA_BASE =
            "https://proyecto.supabase.co/storage/v1/object/public/imagenes-publicas/perfil-it-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private ValoracionRepository valoracionRepository;

    @Autowired
    private PreguntaActividadRepository preguntaActividadRepository;

    @Autowired
    private EventoInteraccionRepository eventoInteraccionRepository;

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
        for (Long actividadId : actividadIds) {
            valoracionRepository.deleteAll(
                    valoracionRepository.findAll().stream()
                            .filter(valoracion -> valoracion.getActividadId().equals(actividadId))
                            .toList());
            preguntaActividadRepository.deleteAll(
                    preguntaActividadRepository.findAll().stream()
                            .filter(pregunta -> pregunta.getActividadId().equals(actividadId))
                            .toList());
        }
        valoracionRepository.flush();
        preguntaActividadRepository.flush();

        for (Long perfilId : perfilPublicadorIds) {
            eventoInteraccionRepository.deleteAll(
                    eventoInteraccionRepository.findAll().stream()
                            .filter(evento -> perfilId.equals(evento.getPerfilPublicadorId()))
                            .toList());
        }
        eventoInteraccionRepository.flush();

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

    /**
     * El corazón de la fase: los números de la cabecera salen de la
     * base real y el promedio respeta el umbral de 3.
     */
    @Test
    void losStatsDeCabeceraSalenDeLaBaseYRespetanElUmbralDelPromedio() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad primera = crearActividadPublicada(perfil, referencias);
        Actividad segunda = crearActividadPublicada(perfil, referencias);
        crearImagenAprobada(primera);
        crearImagenAprobada(segunda);

        /* Con DOS valoraciones el promedio todavía no se publica. */
        crearValoracion(primera, crearUsuario(ROL_USUARIO), 5);
        crearValoracion(segunda, crearUsuario(ROL_USUARIO), 4);

        mockMvc.perform(get("/api/perfiles-publicadores/{id}", perfil.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadActividades").value(2))
                .andExpect(jsonPath("$.cantidadFotos").value(2))
                .andExpect(jsonPath("$.cantidadValoraciones").value(2))
                .andExpect(jsonPath("$.valoracionPromedio").doesNotExist());

        /* Con la tercera, el promedio aparece: (5+4+3)/3 = 4.0 */
        crearValoracion(primera, crearUsuario(ROL_USUARIO), 3);

        mockMvc.perform(get("/api/perfiles-publicadores/{id}", perfil.getId()))
                .andExpect(jsonPath("$.cantidadValoraciones").value(3))
                .andExpect(jsonPath("$.valoracionPromedio").value(4.0));
    }

    /**
     * Las opiniones de TODAS sus actividades juntas, cada una sabiendo
     * de cuál habla. Sin esto, el perfil no muestra ninguna reseña.
     */
    @Test
    void lasOpinionesDelPerfilMezclanActividadesYTraenSuContexto() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad primera = crearActividadPublicada(perfil, referencias);
        Actividad segunda = crearActividadPublicada(perfil, referencias);

        crearValoracion(primera, crearUsuario(ROL_USUARIO), 5);
        crearValoracion(segunda, crearUsuario(ROL_USUARIO), 4);

        mockMvc.perform(get("/api/perfiles-publicadores/{id}/valoraciones", perfil.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].actividadTitulo").exists())
                .andExpect(jsonPath("$.contenido[0].actividadSlug").exists());

        /* Solo las RESPONDIDAS entran al perfil. */
        crearPregunta(primera, crearUsuario(ROL_USUARIO), "¿Hay clases los sábados?", null);
        crearPregunta(segunda, crearUsuario(ROL_USUARIO), "¿Prestan equipamiento?", "Sí, todo incluido.");

        mockMvc.perform(get("/api/perfiles-publicadores/{id}/preguntas", perfil.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].respuesta").value("Sí, todo incluido."))
                .andExpect(jsonPath("$[0].actividadTitulo").exists());
    }

    /** Destacadas: se persisten en orden, reemplazan y validan dueño. */
    @Test
    void lasDestacadasSeReemplazanYValidanContraLaBase() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad primera = crearActividadPublicada(perfil, referencias);
        Actividad segunda = crearActividadPublicada(perfil, referencias);
        Long duenioId = perfil.getUsuario().getId();

        mockMvc.perform(put("/api/publicador/actividades/destacadas")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadIds\":[" + segunda.getId() + "," + primera.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        assertEquals(1, actividadRepository.findById(segunda.getId()).orElseThrow().getDestacadaOrden());
        assertEquals(2, actividadRepository.findById(primera.getId()).orElseThrow().getDestacadaOrden());

        /* En el perfil público salen en ESE orden. */
        mockMvc.perform(get("/api/perfiles-publicadores/{id}/destacadas", perfil.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(segunda.getId()));

        /* Una actividad de OTRO publicador da 404 y no toca nada. */
        PerfilPublicador ajeno = crearPerfilPublicador(referencias.ciudad());
        Actividad deOtro = crearActividadPublicada(ajeno, referencias);

        mockMvc.perform(put("/api/publicador/actividades/destacadas")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadIds\":[" + deOtro.getId() + "]}"))
                .andExpect(status().isNotFound());

        /* Más de 3: 400. */
        mockMvc.perform(put("/api/publicador/actividades/destacadas")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadIds\":[1,2,3,4]}"))
                .andExpect(status().isBadRequest());

        /* Lista vacía limpia la selección. */
        mockMvc.perform(put("/api/publicador/actividades/destacadas")
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertNull(actividadRepository.findById(segunda.getId()).orElseThrow().getDestacadaOrden());
    }

    /**
     * El WhatsApp del perfil ahora se mide, y NO ensucia las métricas
     * por actividad: el evento va con actividad_id en null.
     */
    @Test
    void elContactoDesdeElPerfilSeRegistraSinTocarLasActividades() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias);

        mockMvc.perform(post("/api/perfiles-publicadores/{id}/interacciones", perfil.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"CLICK_WHATSAPP\"}"))
                .andExpect(status().isNoContent());

        List<EventoInteraccion> delPerfil = eventoInteraccionRepository.findAll().stream()
                .filter(evento -> perfil.getId().equals(evento.getPerfilPublicadorId()))
                .toList();

        assertEquals(1, delPerfil.size());
        assertNull(delPerfil.get(0).getActividadId(),
                "El evento del perfil no debe colgar de ninguna actividad.");

        /* La métrica de la actividad sigue en cero: son universos separados. */
        List<EventoInteraccion> deLaActividad = eventoInteraccionRepository.findAll().stream()
                .filter(evento -> actividad.getId().equals(evento.getActividadId()))
                .toList();
        assertTrue(deLaActividad.isEmpty());

        /* Un tipo fuera de catálogo da 400. */
        mockMvc.perform(post("/api/perfiles-publicadores/{id}/interacciones", perfil.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"CLICK_INVENTADO\"}"))
                .andExpect(status().isBadRequest());
    }

    /** El endpoint agregado de fotos: perfil + actividades en UNA llamada. */
    @Test
    void lasFotosDelPublicadorLleganEnUnSoloRequest() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad primera = crearActividadPublicada(perfil, referencias);
        Actividad segunda = crearActividadPublicada(perfil, referencias);
        crearImagenAprobada(primera);
        crearImagenAprobada(segunda);
        crearLogoAprobado(perfil);

        /* El LOGO NO cuenta: es identidad, ya se ve en la cabecera. */
        mockMvc.perform(get("/api/perfiles-publicadores/{id}/fotos", perfil.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/perfiles-publicadores/{id}", perfil.getId()))
                .andExpect(jsonPath("$.cantidadFotos").value(2));
    }

    /* ======================= fixtures ======================= */

    private Valoracion crearValoracion(Actividad actividad, Usuario autor, int puntaje) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Valoracion valoracion = new Valoracion();
        valoracion.setActividadId(actividad.getId());
        valoracion.setUsuarioId(autor.getId());
        valoracion.setPuntaje(puntaje);
        valoracion.setComentario("Opinion del IT de perfil");
        valoracion.setVerificada(false);
        valoracion.setEstado("VISIBLE");
        valoracion.setCreatedAt(ahora);
        valoracion.setUpdatedAt(ahora);

        return valoracionRepository.saveAndFlush(valoracion);
    }

    private PreguntaActividad crearPregunta(
            Actividad actividad,
            Usuario autor,
            String texto,
            String respuesta
    ) {
        OffsetDateTime ahora = OffsetDateTime.now();

        PreguntaActividad pregunta = new PreguntaActividad();
        pregunta.setActividadId(actividad.getId());
        pregunta.setUsuarioId(autor.getId());
        pregunta.setPregunta(texto);
        pregunta.setEstado("VISIBLE");
        pregunta.setCreatedAt(ahora);

        if (respuesta != null) {
            pregunta.setRespuesta(respuesta);
            pregunta.setRespondidaAt(ahora);
        }

        return preguntaActividadRepository.saveAndFlush(pregunta);
    }

    private Imagen crearImagenAprobada(Actividad actividad) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Imagen imagen = new Imagen();
        imagen.setActividad(actividad);
        imagen.setUrl(URL_PUBLICA_BASE + UUID.randomUUID() + ".jpg");
        imagen.setTipoImagen("GALERIA");
        imagen.setOrden(1);
        imagen.setActiva(true);
        imagen.setEstadoModeracion("APROBADA");
        imagen.setCreatedAt(ahora);
        imagen.setUpdatedAt(ahora);

        Imagen guardada = imagenRepository.saveAndFlush(imagen);
        imagenIds.add(guardada.getId());
        return guardada;
    }

    private Imagen crearLogoAprobado(PerfilPublicador perfil) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Imagen imagen = new Imagen();
        imagen.setPerfilPublicador(perfil);
        imagen.setUrl(URL_PUBLICA_BASE + "logo-" + UUID.randomUUID() + ".jpg");
        imagen.setTipoImagen("LOGO");
        imagen.setOrden(1);
        imagen.setActiva(true);
        imagen.setEstadoModeracion("APROBADA");
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
        usuario.setNombre("Usuario Perfil IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("perfil-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-perfil-it");
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
        perfil.setWhatsapp("+54 9 223 555-1401");
        perfil.setWhatsappNormalizado("5492235551401");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para PerfilPublicadorSocialIT."));

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
                        "No hay una ciudad activa con barrio activo para PerfilPublicadorSocialIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Perfil IT");
        ubicacion.setDireccion("Calle perfil 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Perfil IT");
        actividad.setSlug(slugUnico("actividad-perfil-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por PerfilPublicadorSocialIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-1402");
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
                        .subject("perfil-it@dondeentreno.test")
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
