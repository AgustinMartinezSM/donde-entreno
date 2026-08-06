package com.dondeentreno.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador simple para verificar que la API esté funcionando.
 *
 * Este endpoint no consulta la base de datos.
 * Solo sirve para probar que Spring Boot levanta correctamente
 * y que podemos recibir peticiones HTTP.
 */
@RestController
public class HealthController {

    /**
     * Endpoint de salud.
     *
     * GET http://localhost:8080/api/health
     *
     * Devuelve JSON para ser coherente con el resto de la API.
     */
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "donde-entreno-api"
        );
    }
}