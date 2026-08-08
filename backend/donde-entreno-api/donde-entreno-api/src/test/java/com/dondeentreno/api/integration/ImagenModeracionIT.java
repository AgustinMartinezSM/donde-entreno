package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Actividad;
import com.dondeentreno.api.entity.Barrio;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.Deporte;
import com.dondeentreno.api.entity.Imagen;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Ubicacion;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.ActividadRepository;
import com.dondeentreno.api.repository.BarrioRepository;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.DeporteRepository;
import com.dondeentreno.api.repository.ImagenRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
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
import org.springframework.mock.web.MockMultipartFile;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de imágenes con moderación contra PostgreSQL local:
 * el publicador sube (PENDIENTE, invisible en público), el admin
 * aprueba (visible) o rechaza con motivo, y la PRINCIPAL aprobada
 * reemplaza lógicamente a la anterior.
 *
 * El almacenamiento usa una implementación en memoria (bean @Primary
 * de este test): el flujo se prueba completo sin depender de Supabase.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-local")
@org.springframework.context.annotation.Import(ImagenModeracionIT.AlmacenEnMemoriaConfig.class)
@TestPropertySource(properties = {
        "dondeentreno.auth.jwt.secret=test-secret-only-for-imagen-moderacion-it-1234567890",
        "dondeentreno.auth.jwt.issuer=dondeentreno-api-test",
        "dondeentreno.auth.jwt.expiration-minutes=60"
})
class ImagenModeracionIT {

    /**
     * Storage en memoria para la IT: respeta el contrato de dos
     * espacios (privado/público) sin red ni credenciales.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class AlmacenEnMemoriaConfig {

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public com.dondeentreno.api.storage.AlmacenArchivos almacenArchivosEnMemoria() {
            java.util.Map<String, byte[]> pendientes =
                    new java.util.concurrent.ConcurrentHashMap<>();

            return new com.dondeentreno.api.storage.AlmacenArchivos() {
                @Override
                public boolean estaConfigurado() {
                    return true;
                }

                @Override
                public String guardarPendiente(
                        byte[] contenido, String carpetaRelativa, String extension) {
                    String ruta = carpetaRelativa + "/"
                            + java.util.UUID.randomUUID() + "." + extension;
                    pendientes.put(ruta, contenido);
                    return ruta;
                }

                @Override
                public String publicar(String rutaObjeto) {
                    if (!pendientes.containsKey(rutaObjeto)) {
                        throw new IllegalStateException(
                                "El objeto no existe en el espacio privado: " + rutaObjeto);
                    }
                    pendientes.remove(rutaObjeto);
                    return "https://storage.test/publicas/" + rutaObjeto;
                }

                @Override
                public String firmarUrl(String rutaObjeto, java.time.Duration validez) {
                    return "https://storage.test/firmada/" + rutaObjeto;
                }

                @Override
                public void eliminar(String rutaObjeto) {
                    pendientes.remove(rutaObjeto);
                }
            };
        }
    }

    private static final byte[] BYTES_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x20, 0x30, 0x40};
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
    private ImagenRepository imagenRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private BarrioRepository barrioRepository;

    @Autowired
    private DeporteRepository deporteRepository;

    private final List<Long> imagenIds = new ArrayList<>();
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
        registrarImagenesDeActividades();

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

        for (String marcador : marcadores) {
            assertFalse(existeResiduoConMarcador(marcador), "Quedaron datos residuales del test: " + marcador);
        }

        imagenIds.clear();
        actividadIds.clear();
        ubicacionIds.clear();
        perfilPublicadorIds.clear();
        usuarioIds.clear();
        marcadores.clear();
    }

    @Test
    void flujoCompletoSubirAprobarYVisibilidadPublica() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Usuario admin = crearAdmin(marcador);
        Actividad actividad = crearActividadPublicada(marcador, publicador.perfil(), referencias);

        // 1) El publicador sube una imagen PRINCIPAL: nace PENDIENTE e inactiva.
        ResultActions subida = mockMvc.perform(
                        multipart("/api/publicador/actividades/" + actividad.getId() + "/imagenes")
                                .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", BYTES_JPEG))
                                .param("tipo", "PRINCIPAL")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoModeracion").value("PENDIENTE"))
                .andExpect(jsonPath("$.activa").value(false));

        long primeraImagenId = leerJson(subida).path("id").asLong();
        imagenIds.add(primeraImagenId);

        // 2) En público la imagen pendiente NO se ve.
        JsonNode imagenesPublicas = leerJson(
                mockMvc.perform(get("/api/actividades/" + actividad.getSlug() + "/imagenes"))
                        .andExpect(status().isOk())
        );
        assertFalse(listaContieneId(imagenesPublicas, primeraImagenId),
                "Una imagen PENDIENTE no debe verse en el endpoint publico.");

        // 3) El admin la ve en su cola de pendientes.
        mockMvc.perform(get("/api/admin/imagenes")
                        .param("estado", "PENDIENTE")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk());

        // 4) El admin aprueba: queda activa y visible en público.
        mockMvc.perform(post("/api/admin/imagenes/" + primeraImagenId + "/aprobar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoModeracion").value("APROBADA"))
                .andExpect(jsonPath("$.activa").value(true));

        JsonNode imagenesTrasAprobar = leerJson(
                mockMvc.perform(get("/api/actividades/" + actividad.getSlug() + "/imagenes"))
                        .andExpect(status().isOk())
        );
        assertTrue(listaContieneId(imagenesTrasAprobar, primeraImagenId),
                "Una imagen APROBADA debe verse en el endpoint publico.");

        // 5) Se aprueba una segunda PRINCIPAL: la primera queda inactiva.
        ResultActions segundaSubida = mockMvc.perform(
                        multipart("/api/publicador/actividades/" + actividad.getId() + "/imagenes")
                                .file(new MockMultipartFile("archivo", "foto2.jpg", "image/jpeg", BYTES_JPEG))
                                .param("tipo", "PRINCIPAL")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isCreated());
        long segundaImagenId = leerJson(segundaSubida).path("id").asLong();
        imagenIds.add(segundaImagenId);

        mockMvc.perform(post("/api/admin/imagenes/" + segundaImagenId + "/aprobar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId())))
                .andExpect(status().isOk());

        Imagen primeraImagen = imagenRepository.findById(primeraImagenId).orElseThrow();
        assertFalse(primeraImagen.getActiva(),
                "Al aprobar una nueva PRINCIPAL, la anterior debe quedar inactiva.");

        JsonNode imagenesFinales = leerJson(
                mockMvc.perform(get("/api/actividades/" + actividad.getSlug() + "/imagenes"))
                        .andExpect(status().isOk())
        );
        assertTrue(listaContieneId(imagenesFinales, segundaImagenId));
        assertFalse(listaContieneId(imagenesFinales, primeraImagenId),
                "La PRINCIPAL reemplazada no debe seguir visible en publico.");
    }

    @Test
    void rechazarRequiereMotivoYLaImagenNoSeVeEnPublico() throws Exception {
        Referencias referencias = obtenerReferenciasActivas();
        String marcador = marcadorUnico();
        Publicador publicador = crearPublicador(marcador, referencias.ciudad());
        Usuario admin = crearAdmin(marcador);
        Actividad actividad = crearActividadPublicada(marcador, publicador.perfil(), referencias);

        ResultActions subida = mockMvc.perform(
                        multipart("/api/publicador/actividades/" + actividad.getId() + "/imagenes")
                                .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", BYTES_JPEG))
                                .param("tipo", "GALERIA")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isCreated());
        long imagenId = leerJson(subida).path("id").asLong();
        imagenIds.add(imagenId);

        // Rechazo sin motivo: 400 por Bean Validation.
        mockMvc.perform(post("/api/admin/imagenes/" + imagenId + "/rechazar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Rechazo con motivo: queda RECHAZADA, inactiva y fuera de público.
        mockMvc.perform(post("/api/admin/imagenes/" + imagenId + "/rechazar")
                        .with(jwtConRol(ROL_ADMIN, admin.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"La imagen no corresponde a la actividad.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoModeracion").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("La imagen no corresponde a la actividad."));

        JsonNode imagenesPublicas = leerJson(
                mockMvc.perform(get("/api/actividades/" + actividad.getSlug() + "/imagenes"))
                        .andExpect(status().isOk())
        );
        assertFalse(listaContieneId(imagenesPublicas, imagenId));

        // El archivo que no es imagen (PDF disfrazado) se rechaza al subir.
        mockMvc.perform(
                        multipart("/api/publicador/actividades/" + actividad.getId() + "/imagenes")
                                .file(new MockMultipartFile(
                                        "archivo",
                                        "falso.jpg",
                                        "image/jpeg",
                                        new byte[] {0x25, 0x50, 0x44, 0x46}
                                ))
                                .param("tipo", "GALERIA")
                                .with(jwtConRol(ROL_PUBLICADOR, publicador.usuario().getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endpointsDeImagenesRespetanRoles() throws Exception {
        mockMvc.perform(get("/api/admin/imagenes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/imagenes")
                        .with(jwtConRol(ROL_PUBLICADOR, Long.MAX_VALUE - 21)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/publicador/actividades/1/imagenes")
                        .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", BYTES_JPEG))
                        .param("tipo", "GALERIA"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/publicador/actividades/1/imagenes")
                        .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", BYTES_JPEG))
                        .param("tipo", "GALERIA")
                        .with(jwtConRol(ROL_USUARIO, Long.MAX_VALUE - 22)))
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
        usuario.setApellido("Imagen IT");
        usuario.setEmail("imagen-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-imagen-it");
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
        perfil.setWhatsapp("+54 9 223 555-0301");
        perfil.setWhatsappNormalizado("5492235550301");
        perfil.setTelefonoContacto("+54 9 223 555-0302");
        perfil.setTelefonoContactoNormalizado("5492235550302");
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
        usuario.setApellido("Imagen IT");
        usuario.setEmail("admin-imagen-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-admin-imagen-it");
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
        ubicacion.setDireccion("Calle imagen " + marcador);
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
        actividad.setSlug(slugUnico("actividad-imagen-" + marcador));
        actividad.setDescripcion("Actividad creada por ImagenModeracionIT " + marcador);
        actividad.setEdadMinima(18);
        actividad.setEdadMaxima(65);
        actividad.setNivel("PRINCIPIANTE");
        actividad.setEnfoque("RECREATIVO");
        actividad.setModalidad("PRESENCIAL");
        actividad.setPrecioReferencia(new BigDecimal("15000.00"));
        actividad.setMostrarPrecio(true);
        actividad.setRequiereInscripcion(false);
        actividad.setCuposLimitados(false);
        actividad.setWhatsappContacto("+54 9 223 555-0303");
        actividad.setInstagramContacto("@imagen_it");
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
                .orElseThrow(() -> new IllegalStateException("No hay deportes activos para ImagenModeracionIT."));

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
                        "No hay una ciudad activa con barrio activo para ImagenModeracionIT."
                ));
    }

    private JsonNode leerJson(ResultActions resultActions) throws Exception {
        String response = resultActions.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private boolean listaContieneId(JsonNode lista, long id) {
        if (!lista.isArray()) {
            return false;
        }

        for (JsonNode item : lista) {
            if (item.path("id").asLong() == id) {
                return true;
            }
        }

        return false;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol, Long userId) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("imagen-it@dondeentreno.test")
                        .claim("userId", userId)
                        .claim("rol", rol)
                        .claim("roles", List.of(rol))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private void registrarImagenesDeActividades() {
        for (Long actividadId : actividadIds) {
            imagenRepository.findByActividad_IdOrderByCreatedAtDesc(actividadId).stream()
                    .map(Imagen::getId)
                    .filter(id -> !imagenIds.contains(id))
                    .forEach(imagenIds::add);
        }
    }

    private boolean existeResiduoConMarcador(String marcador) {
        return actividadRepository.findAll().stream()
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
        String marcador = "IT-IMAGEN-MODERACION-" + UUID.randomUUID();
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
}
