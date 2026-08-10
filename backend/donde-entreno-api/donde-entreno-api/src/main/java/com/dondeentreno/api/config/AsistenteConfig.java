package com.dondeentreno.api.config;

import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.LimitadorConsultas;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Config del asistente.
 *
 * Igual que el almacenamiento: los beans existen siempre, configurado o
 * no. Sin variables de entorno el asistente responde con el motor local
 * y Gemini queda apagado, así que el deploy es tolerante al orden.
 *
 * El reloj se pasa por constructor y no como bean, igual que en
 * JwtService: los tests construyen el limitador con un Clock fijo y no
 * hace falta publicar un Clock global que después compita en otras
 * inyecciones por tipo.
 */
@Configuration
@EnableConfigurationProperties(AsistenteProperties.class)
public class AsistenteConfig {

    @Bean
    public LimitadorConsultas limitadorConsultas(AsistenteProperties propiedades) {
        return new LimitadorConsultas(propiedades, Clock.systemUTC());
    }
}
