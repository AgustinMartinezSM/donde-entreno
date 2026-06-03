package com.dondeentreno.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Endpoint de prueba.
     *
     * Cuando alguien entra a:
     * GET http://localhost:8080/api/health
     *
     * Spring Boot ejecuta este método y devuelve el texto.
     */
    @GetMapping("/api/health")
    public String health() {
        return "DondeEntreno API funcionando correctamente";
    }
}