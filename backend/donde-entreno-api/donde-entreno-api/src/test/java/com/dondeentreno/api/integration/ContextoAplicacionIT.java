package com.dondeentreno.api.integration;

import com.dondeentreno.api.controller.ActividadController;
import com.dondeentreno.api.exception.GlobalExceptionHandler;
import com.dondeentreno.api.service.ActividadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integracion que valida el arranque real del contexto de Spring.
 *
 * mvn test (surefire) no levanta el contexto completo, asi que este IT
 * es el que detecta errores de wiring: beans faltantes, dependencias
 * circulares, properties mal definidas, etc.
 *
 * Corre con failsafe bajo -Pintegration-local, igual que el resto de los *IT.
 */
@SpringBootTest
@ActiveProfiles("integration-local")
class ContextoAplicacionIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @BeforeEach
    void verificarDatasourceLocal() {
        verificarVariablesLocalesPresentes();

        String url = environment.getProperty("spring.datasource.url", "");
        assertTrue(esDatasourceLocal(url), "El perfil integration-local debe apuntar solo a localhost o 127.0.0.1.");
        assertFalse(esDatasourceRemoto(url), "El perfil integration-local no debe apuntar a Supabase, Render ni hosts remotos.");
    }

    @Test
    void elContextoArrancaYContieneBeansClave() {
        assertNotNull(applicationContext, "El contexto de Spring debe arrancar.");

        assertNotNull(
                applicationContext.getBean(ActividadController.class),
                "El controller de actividades debe estar registrado en el contexto."
        );
        assertNotNull(
                applicationContext.getBean(ActividadService.class),
                "El service de actividades debe estar registrado en el contexto."
        );
        assertNotNull(
                applicationContext.getBean(GlobalExceptionHandler.class),
                "El manejador global de errores debe estar registrado en el contexto."
        );
        // Se busca por nombre porque Spring MVC registra ademas
        // mvcHandlerMappingIntrospector, que tambien implementa la interfaz.
        // Spring Security resuelve el bean por este mismo nombre.
        assertNotNull(
                applicationContext.getBean("corsConfigurationSource", CorsConfigurationSource.class),
                "La configuracion CORS consolidada debe estar registrada en el contexto."
        );
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
}
