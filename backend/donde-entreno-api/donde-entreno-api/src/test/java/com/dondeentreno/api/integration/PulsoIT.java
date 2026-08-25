package com.dondeentreno.api.integration;

import com.dondeentreno.api.dto.PulsoDTO;
import com.dondeentreno.api.service.PulsoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El pulso (Fase 10, paso 0).
 *
 * Este IT existe por una razón concreta: el service usa **SQL nativo**,
 * así que un nombre de tabla o de columna equivocado NO lo detecta el
 * compilador. Acá se ejecutan las 22 consultas contra el schema real.
 *
 * Y como cada conteo está aislado en un try/catch que devuelve 0, un
 * error de SQL sería INVISIBLE en producción: el panel mostraría ceros
 * y parecería que "no hay uso". Por eso el test no se conforma con que
 * responda 200 — verifica que los números que TIENEN que ser mayores a
 * cero lo sean.
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
class PulsoIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private PulsoService pulsoService;

    @BeforeEach
    void verificarDatasourceLocal() {
        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    /**
     * Las 22 consultas corren de verdad.
     *
     * La base local tiene el seed, así que actividades, publicadores y
     * usuarios TIENEN que dar > 0: si alguna diera 0, es que el SQL
     * falló y el catch lo tapó.
     */
    @Test
    void lasConsultasDelPulsoCorrenContraElSchemaReal() {
        PulsoDTO pulso = pulsoService.obtener();

        assertNotNull(pulso.getBloques());
        assertTrue(pulso.getBloques().size() >= 3, "Faltan bloques del pulso.");

        List<PulsoDTO.Metrica> catalogo = pulso.getBloques().get(0).getMetricas();

        for (PulsoDTO.Metrica metrica : catalogo) {
            assertNotNull(metrica.getTotal(), "Métrica sin total: " + metrica.getEtiqueta());
        }

        /*
          El seed tiene actividades, publicadores y usuarios. Un 0 acá
          significaría que la query se rompió y el catch la silenció.
        */
        assertTrue(valorDe(catalogo, "Actividades publicadas") > 0,
                "El seed tiene actividades: un 0 significa que el SQL falló y el catch lo tapó.");
        assertTrue(valorDe(catalogo, "Publicadores activos") > 0,
                "El seed tiene publicadores: un 0 significa que el SQL falló.");
        assertTrue(valorDe(catalogo, "Usuarios") > 0,
                "El seed tiene usuarios: un 0 significa que el SQL falló.");
    }

    /** Las tablas de las fases sociales también se consultan bien. */
    @Test
    void elBloqueSocialSeResuelveSinRomperse() {
        PulsoDTO pulso = pulsoService.obtener();
        List<PulsoDTO.Metrica> social = pulso.getBloques().get(1).getMetricas();

        assertFalse(social.isEmpty());

        /*
          Acá los ceros son legítimos (puede no haber novedades ni
          grupos todavía), pero el total nunca puede ser null: eso
          sería un bug de armado, no un dato.
        */
        for (PulsoDTO.Metrica metrica : social) {
            assertNotNull(metrica.getTotal(), "Métrica sin total: " + metrica.getEtiqueta());
            assertTrue(metrica.getTotal() >= 0);
        }
    }

    @Test
    void elPulsoEsSoloParaAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/pulso"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/pulso").with(jwtConRol("USUARIO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/pulso").with(jwtConRol("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloques[0].titulo").value("Catálogo"));
    }

    private long valorDe(List<PulsoDTO.Metrica> metricas, String etiqueta) {
        return metricas.stream()
                .filter(metrica -> etiqueta.equals(metrica.getEtiqueta()))
                .map(PulsoDTO.Metrica::getTotal)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe la métrica " + etiqueta));
    }

    private RequestPostProcessor jwtConRol(String rol) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("pulso-it@dondeentreno.test")
                        .claim("userId", 1)
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
