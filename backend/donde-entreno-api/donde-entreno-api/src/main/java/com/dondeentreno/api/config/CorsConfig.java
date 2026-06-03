package com.dondeentreno.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración global de CORS.
 *
 * CORS permite que el frontend pueda hacer peticiones
 * al backend aunque estén corriendo en puertos distintos.
 *
 * Ejemplo:
 * Frontend Next.js: http://localhost:3000
 * Backend Spring Boot: http://localhost:8080
 */
@Configuration
public class CorsConfig {

    /**
     * Bean que configura reglas CORS para toda la API.
     *
     * Por ahora permitimos el origen local de Next.js:
     * http://localhost:3000
     *
     * Más adelante, cuando subamos el proyecto,
     * vamos a agregar el dominio real del frontend.
     *
     * @return configuración CORS personalizada.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            /**
             * Define qué rutas aceptan peticiones desde otros orígenes.
             *
             * @param registry registro de configuración CORS.
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
