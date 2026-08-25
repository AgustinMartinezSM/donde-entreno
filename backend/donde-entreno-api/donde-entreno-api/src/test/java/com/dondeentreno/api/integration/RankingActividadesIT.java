package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.EventoInteraccion;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.EventoInteraccionRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ranking semanal de actividades (Fase 10).
 *
 * La regla del proyecto: endpoint nuevo, IT propio, y que verifique lo
 * que el endpoint PROMETE — no que responda 200. Acá eso significa tres
 * cosas distintas:
 *
 * 1. Que ORDENE por vistas de verdad (un ranking que responde 200 con
 *    el orden equivocado es peor que no tenerlo).
 * 2. Que una actividad DESPUBLICADA no se cuele, aunque el tracking
 *    le sobreviva a la publicación.
 * 3. Que los eventos fuera de la ventana no cuenten.
 *
 * El UMBRAL (menos de N actividades con señal = sección apagada) NO se
 * prueba acá: depende del estado global de la base, así que un IT que
 * lo afirmara sería verde o rojo según los datos ajenos que hubiera.
 * Va como unit test, con el repositorio mockeado.
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
class RankingActividadesIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String VISTA_DETALLE = "VISTA_DETALLE";

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

    @Autowired
    private EventoInteraccionRepository eventoInteraccionRepository;

    private final List<Long> eventoIds = new ArrayList<>();
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
        for (Long eventoId : eventoIds) {
            eventoInteraccionRepository.findById(eventoId).ifPresent(eventoInteraccionRepository::delete);
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

        eventoIds.clear();

        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }
    /**
     * EL test del ranking: con tres actividades y distinta cantidad de
     * vistas, salen ORDENADAS de más a menos.
     *
     * Un endpoint que las devuelve todas pero en cualquier orden pasa
     * un test de "responde 200" y está roto igual.
     *
     * Se comparan POSICIONES RELATIVAS y no absolutas a propósito: la
     * base local puede tener otras actividades con tracking real, y un
     * test que exigiera el podio exacto fallaría por datos ajenos.
     */
    @Test
    void elRankingDevuelveLasMasVistasPrimero() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());

        Actividad pocoVista = crearActividadPublicada(perfil, referencias);
        Actividad masVista = crearActividadPublicada(perfil, referencias);
        Actividad intermedia = crearActividadPublicada(perfil, referencias);

        registrarVistas(masVista, 9);
        registrarVistas(intermedia, 5);
        registrarVistas(pocoVista, 2);

        String cuerpo = mockMvc.perform(get("/api/actividades/mas-vistas")
                        .param("dias", "7")
                        .param("limite", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int posicionMasVista = cuerpo.indexOf(masVista.getSlug());
        int posicionIntermedia = cuerpo.indexOf(intermedia.getSlug());
        int posicionPocoVista = cuerpo.indexOf(pocoVista.getSlug());

        assertTrue(posicionMasVista >= 0, "La más vista tiene que estar en el ranking.");
        assertTrue(posicionIntermedia >= 0, "La intermedia tiene que estar en el ranking.");
        assertTrue(posicionPocoVista >= 0, "La menos vista tiene que estar en el ranking.");

        assertTrue(posicionMasVista < posicionIntermedia,
                "La de 9 vistas tiene que ir antes que la de 5.");
        assertTrue(posicionIntermedia < posicionPocoVista,
                "La de 5 vistas tiene que ir antes que la de 2.");
    }

    /**
     * Una actividad DESPUBLICADA no entra, aunque sea la más vista: el
     * tracking le sobrevive a la publicación.
     *
     * El test prueba PRIMERO que aparece publicada. Un test que solo
     * verifica una desaparición pasa igual si nunca apareció — es la
     * lección que dejó el falso verde de la Fase 8.
     */
    @Test
    void unaActividadDespublicadaNoEntraAlRanking() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());

        Actividad candidata = crearActividadPublicada(perfil, referencias);
        Actividad acompanante = crearActividadPublicada(perfil, referencias);
        Actividad tercera = crearActividadPublicada(perfil, referencias);
        Actividad cuarta = crearActividadPublicada(perfil, referencias);

        registrarVistas(candidata, 12);
        registrarVistas(acompanante, 4);
        registrarVistas(tercera, 3);
        /*
          La CUARTA no es decorativa: sin ella, al despublicar la
          candidata quedan dos con señal y el UMBRAL apaga la sección
          entera. El test parecería decir "se cayó el ranking" cuando en
          realidad estaba haciendo lo que debe.
        */
        registrarVistas(cuarta, 2);

        /* Primero: publicada, ESTÁ en el ranking. */
        String conLaActividad = mockMvc.perform(get("/api/actividades/mas-vistas")
                        .param("dias", "7")
                        .param("limite", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(conLaActividad.contains(candidata.getSlug()),
                "Publicada y con 12 vistas, tiene que estar.");

        candidata.setEstadoPublicacion("BORRADOR");
        actividadRepository.saveAndFlush(candidata);

        String sinLaActividad = mockMvc.perform(get("/api/actividades/mas-vistas")
                        .param("dias", "7")
                        .param("limite", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(sinLaActividad.contains(candidata.getSlug()),
                "Despublicada, no puede seguir en el ranking.");
        assertTrue(sinLaActividad.contains(acompanante.getSlug()),
                "Las otras siguen: no se cayó el ranking entero.");
    }

    /**
     * Los eventos FUERA de la ventana no cuentan.
     *
     * Es lo que hace que "lo más visto esta semana" signifique algo:
     * sin este filtro, una actividad que fue un éxito hace seis meses
     * encabezaría el ranking para siempre.
     */
    @Test
    void losEventosViejosNoEntranEnLaVentana() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        PerfilPublicador perfil = crearPerfilPublicador(referencias.ciudad());

        Actividad unaVieja = crearActividadPublicada(perfil, referencias);
        Actividad otraVieja = crearActividadPublicada(perfil, referencias);
        Actividad terceraVieja = crearActividadPublicada(perfil, referencias);

        registrarVistasHaceDias(unaVieja, 40, 60);
        registrarVistasHaceDias(otraVieja, 30, 60);
        registrarVistasHaceDias(terceraVieja, 20, 60);

        String cuerpo = mockMvc.perform(get("/api/actividades/mas-vistas")
                        .param("dias", "7")
                        .param("limite", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(cuerpo.contains(unaVieja.getSlug()),
                "Vistas de hace 60 días no pueden contar para la semana.");
        assertFalse(cuerpo.contains(otraVieja.getSlug()),
                "Vistas de hace 60 días no pueden contar para la semana.");
        assertFalse(cuerpo.contains(terceraVieja.getSlug()),
                "Vistas de hace 60 días no pueden contar para la semana.");
    }

    private void registrarVistas(Actividad actividad, int cantidad) {
        registrarVistasHaceDias(actividad, cantidad, 0);
    }

    private void registrarVistasHaceDias(Actividad actividad, int cantidad, int diasAtras) {
        OffsetDateTime momento = OffsetDateTime.now().minusDays(diasAtras).minusHours(1);

        for (int i = 0; i < cantidad; i++) {
            EventoInteraccion evento = new EventoInteraccion();
            evento.setActividadId(actividad.getId());
            evento.setPerfilPublicadorId(actividad.getPerfilPublicador().getId());
            evento.setTipo(VISTA_DETALLE);
            evento.setCreatedAt(momento);

            eventoIds.add(eventoInteraccionRepository.saveAndFlush(evento).getId());
        }
    }

    private Referencias obtenerReferenciasActivas() {
        Deporte deporte = deporteRepository.findByActivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para RankingActividadesIT."));

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
                        "No hay una ciudad activa con barrio activo para RankingActividadesIT."
                ));
    }

    private Actividad crearActividadPublicada(PerfilPublicador perfil, Referencias referencias) {
        OffsetDateTime ahora = OffsetDateTime.now();

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setPerfilPublicador(perfil);
        ubicacion.setCiudad(referencias.ciudad());
        ubicacion.setBarrio(referencias.barrio());
        ubicacion.setNombre("Sede Ranking IT");
        ubicacion.setDireccion("Calle ranking 123");
        ubicacion.setActiva(true);
        ubicacion.setCreatedAt(ahora);
        ubicacion.setUpdatedAt(ahora);

        Ubicacion ubicacionGuardada = ubicacionRepository.saveAndFlush(ubicacion);
        ubicacionIds.add(ubicacionGuardada.getId());

        Actividad actividad = new Actividad();
        actividad.setPerfilPublicador(perfil);
        actividad.setDeporte(referencias.deporte());
        actividad.setUbicacion(ubicacionGuardada);
        actividad.setTitulo("Actividad Ranking IT");
        actividad.setSlug(slugUnico("actividad-ranking-it-" + UUID.randomUUID()));
        actividad.setDescripcion("Actividad creada por RankingActividadesIT");
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

    private PerfilPublicador crearPerfilPublicador(Ciudad ciudad) {
        Usuario duenio = crearUsuario();
        OffsetDateTime ahora = OffsetDateTime.now();

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Ranking IT " + UUID.randomUUID());
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

    private Usuario crearUsuario() {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_PUBLICADOR)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol " + ROL_PUBLICADOR + " para integration-local."
                ));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Ranking IT");
        usuario.setApellido(ROL_PUBLICADOR);
        usuario.setEmail("ranking-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-ranking-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private String slugUnico(String base) {
        return base.toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        String urlNormalizada = url.toLowerCase();
        return urlNormalizada.contains("supabase")
                || urlNormalizada.contains("render")
                || urlNormalizada.contains("pooler");
    }

    private record Referencias(Deporte deporte, Ciudad ciudad, Barrio barrio) {
    }
}
