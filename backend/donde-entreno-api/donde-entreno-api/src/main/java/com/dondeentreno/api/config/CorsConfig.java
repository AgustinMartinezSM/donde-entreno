package com.dondeentreno.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion global de CORS.
 *
 * CORS permite que el frontend pueda hacer peticiones
 * al backend aunque esten corriendo en dominios o puertos distintos.
 *
 * Ejemplo local:
 * Frontend Next.js: http://localhost:3000
 * Backend Spring Boot: http://localhost:8080
 */
@Configuration
public class CorsConfig {

    /**
     * Origenes permitidos para consumir la API.
     *
     * En local se usa:
     * http://localhost:3000
     *
     * En produccion se puede configurar con la URL publica del frontend.
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    /**
     * Bean que configura reglas CORS para toda la API.
     *
     * @return configuracion CORS personalizada.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            /**
             * Define que rutas aceptan peticiones desde otros origenes.
             *
             * @param registry registro de configuracion CORS.
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
