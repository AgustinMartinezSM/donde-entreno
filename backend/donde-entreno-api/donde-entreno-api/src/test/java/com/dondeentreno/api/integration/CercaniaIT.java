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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cercanía y coordenadas (script 33, Fase 7).
 *
 * Este IT existe por un bug REAL que se escapó al deploy: el modo
 * "cerca mío" devolvía SIEMPRE vacío porque el query de búsqueda
 * espera `texto = ''` y se le pasaba null, así que ninguna fila
 * pasaba el filtro. Los unit tests no podían verlo (mockean el repo)
 * y no había IT del endpoint. Ahora sí.
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
class CercaniaIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";

    /* Punto de referencia: Av. Independencia 3030, Mar del Plata. */
    private static final BigDecimal LAT_BASE = new BigDecimal("-38.005477");
    private static final BigDecimal LNG_BASE = new BigDecimal("-57.542611");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilPublicadorRepository perfilPublicadorRepository;

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

    /**
     * EL test que faltaba: con una actividad con coordenadas cerca del
     * punto consultado, el endpoint TIENE que devolverla. Antes del fix
     * devolvía vacío siempre.
     */
    @Test
    void cercaDevuelveLaActividadConCoordenadasYSuDistancia() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        crearActividadPublicada(perfil, referencias, LAT_BASE, LNG_BASE);

        mockMvc.perform(get("/api/actividades/cerca")
                        .param("lat", LAT_BASE.toPlainString())
                        .param("lng", LNG_BASE.toPlainString())
                        .param("radioKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnRadio", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.contenido[0].distanciaKm").exists());
    }

    /**
     * Una actividad SIN punto cargado no se ubica en ningún lado: queda
     * fuera y se informa cuántas son (nunca en el centro del barrio).
     */
    @Test
    void lasActividadesSinCoordenadasQuedanFueraYSeInforman() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        crearActividadPublicada(perfil, referencias, null, null);

        mockMvc.perform(get("/api/actividades/cerca")
                        .param("lat", LAT_BASE.toPlainString())
                        .param("lng", LNG_BASE.toPlainString())
                        .param("radioKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinCoordenadas", greaterThanOrEqualTo(1)));
    }

    /** Lo que está lejos del radio no entra. */
    @Test
    void loQueEstaFueraDelRadioNoAparece() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        /* ~110 km al norte del punto base. */
        crearActividadPublicada(
                perfil,
                referencias,
                new BigDecimal("-37.005477"),
                LNG_BASE
        );

        var respuesta = mockMvc.perform(get("/api/actividades/cerca")
                        .param("lat", LAT_BASE.toPlainString())
                        .param("lng", LNG_BASE.toPlainString())
                        .param("radioKm", "5"))
                .andExpect(status().isOk())
                .andReturn();

        String cuerpo = respuesta.getResponse().getContentAsString();
        assertFalse(
                cuerpo.contains("Actividad Cercania IT"),
                "Una actividad a 110 km no puede aparecer en un radio de 5 km."
        );
    }

    /** El publicador carga el punto de su sede pegando el link de Maps. */
    @Test
    void elPublicadorCargaElPuntoDeSuSedeConUnLinkDeMaps() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        Actividad actividad = crearActividadPublicada(perfil, referencias, null, null);
        Long ubicacionId = actividad.getUbicacion().getId();
        Long duenioId = perfil.getUsuario().getId();

        mockMvc.perform(patch("/api/publicador/ubicaciones/{id}/coordenadas", ubicacionId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pegado\":\"https://www.google.com/maps/place/Club/"
                                + "@-38.005477,-57.542611,17z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitud").exists());

        Ubicacion recargada = ubicacionRepository.findById(ubicacionId).orElseThrow();
        assertNotNull(recargada.getLatitud(), "El punto tiene que haber quedado guardado.");

        /* Un texto sin coordenadas da 400 con mensaje, no 500. */
        mockMvc.perform(patch("/api/publicador/ubicaciones/{id}/coordenadas", ubicacionId)
                        .with(jwtConRol(ROL_PUBLICADOR, duenioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pegado\":\"Av. Independencia 3030\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Zonas: cuenta actividades por barrio, sin depender de coordenadas. */
    @Test
    void lasZonasCuentanActividadesPorBarrio() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());
        crearActividadPublicada(perfil, referencias, null, null);

        mockMvc.perform(get("/api/actividades/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barrioNombre").exists())
                .andExpect(jsonPath("$[0].cantidadActividades", greaterThanOrEqualTo(1)));
    }

    @Test
    void lasUbicacionesDelPublicadorExigenSesion() throws Exception {
        mockMvc.perform(get("/api/publicador/ubicaciones"))
                .andExpect(status().isUnauthorized());
    }

    /* ======================= fixtures ======================= */

    private Usuario crearUsuario(String rolNombre) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Cercania IT");
        usuario.setApellido(rolNombre);
        usuario.setEmail("cercania-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-cercania-it");
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
        perfil.setNombre("Perfil Cercania IT " + UUID.randomUUID());
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1601");
        perfil.setWhatsappNormalizado("5492235551601");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para CercaniaIT."));

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
                        "No hay una ciudad activa con barrio activo para CercaniaIT."
                ));
    }

    private Actividad crearActividadPublicada(
            PerfilPublicador perfil,
            Referencias referencias,
            BigDecimal latitud,
            BigDecimal longitud
    ) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Cercania IT");
        ubicacion.setDireccion("Calle cercania 123");
        ubicacion.setLatitud(latitud);
        ubicacion.setLongitud(longitud);
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Cercania IT");
        actividad.setSlug(slugUnico("actividad-cercania-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por CercaniaIT");
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-1602");
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
                        .subject("cercania-it@dondeentreno.test")
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
