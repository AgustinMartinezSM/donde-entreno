package com.dondeentreno.api.integration;

import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resolución del perfil público por slug (script 27): por slug 200,
 * por id sigue andando y trae el slug en el DTO, inexistente 404.
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
class PerfilPublicadorSlugIT {

    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String BASE = "/api/perfiles-publicadores";

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
        for (Long perfilPublicadorId : perfilPublicadorIds) {
            perfilPublicadorRepository.findById(perfilPublicadorId).ifPresent(perfilPublicadorRepository::delete);
        }
        perfilPublicadorRepository.flush();

        for (Long usuarioId : usuarioIds) {
            usuarioRepository.findById(usuarioId).ifPresent(usuarioRepository::delete);
        }
        usuarioRepository.flush();

        perfilPublicadorIds.clear();
        usuarioIds.clear();
    }

    @Test
    void elPerfilSeResuelvePorSlugYPorIdConElSlugEnElDto() throws Exception {
        String slug = "slug-it-" + UUID.randomUUID();
        PerfilPublicador perfil = crearPerfilActivo(slug);

        /* Por slug (la URL nueva). */
        mockMvc.perform(get(BASE + "/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(perfil.getId()))
                .andExpect(jsonPath("$.slug").value(slug));

        /* Por id (los links viejos): sigue 200 y trae el slug. */
        mockMvc.perform(get(BASE + "/{id}", perfil.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));

        /* El listado publico tambien expone el slug. */
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.id == " + perfil.getId() + ")].slug",
                        org.hamcrest.Matchers.hasItem(slug)
                ));

        /* Slug inexistente: 404, no 400 (el path ya no exige numero). */
        mockMvc.perform(get(BASE + "/{slug}", "no-existe-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private PerfilPublicador crearPerfilActivo(String slug) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Rol rol = rolRepository.findByNombre(ROL_PUBLICADOR)
                .orElseThrow(() -> new IllegalStateException("No existe el rol PUBLICADOR para integration-local."));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario Slug IT");
        usuario.setApellido("Publicador");
        usuario.setEmail("slug-it-" + UUID.randomUUID() + "@dondeentreno.test");
        usuario.setPasswordHash("hash-ficticio-slug-it");
        usuario.setTelefonoVerificado(false);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);
        Usuario duenio = usuarioRepository.saveAndFlush(usuario);
        usuarioIds.add(duenio.getId());

        Ciudad ciudad = ciudadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay ciudades activas para PerfilPublicadorSlugIT."));

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(duenio);
        perfil.setNombre("Perfil Slug IT " + UUID.randomUUID());
        perfil.setSlug(slug);
        perfil.setTipoPublicador("ESCUELA_DEPORTIVA");
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudad);
        perfil.setEmailContacto(duenio.getEmail());
        perfil.setWhatsapp("+54 9 223 555-1001");
        perfil.setWhatsappNormalizado("5492235551001");
        perfil.setActivo(true);
        perfil.setVerificado(false);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);

        PerfilPublicador guardado = perfilPublicadorRepository.saveAndFlush(perfil);
        perfilPublicadorIds.add(guardado.getId());
        return guardado;
    }

    private boolean esDatasourceLocal(String url) {
        return url.matches("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/.*");
    }

    private boolean esDatasourceRemoto(String url) {
        String urlNormalizada = url.toLowerCase();
        return urlNormalizada.contains("supabase") || urlNormalizada.contains("render") || urlNormalizada.contains("pooler");
    }
}
