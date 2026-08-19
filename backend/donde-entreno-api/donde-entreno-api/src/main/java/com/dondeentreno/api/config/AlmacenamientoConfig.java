package com.dondeentreno.api.config;

import com.dondeentreno.api.storage.AlmacenArchivos;
import com.dondeentreno.api.storage.AlmacenArchivosProperties;
import com.dondeentreno.api.storage.AlmacenArchivosSupabase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Config del almacenamiento de archivos (Supabase Storage).
 *
 * El bean existe siempre, configurado o no: sin las variables de
 * entorno la app arranca igual y las operaciones de storage responden
 * 503 con un mensaje claro (AlmacenNoConfiguradoException). Eso hace
 * el deploy tolerante al orden: primero puede salir el código y
 * después configurarse el panel, o al revés.
 */
@Configuration
@EnableConfigurationProperties({AlmacenArchivosProperties.class, MediaProperties.class})
public class AlmacenamientoConfig {

    @Bean
    public AlmacenArchivos almacenArchivos(AlmacenArchivosProperties propiedades) {
        return new AlmacenArchivosSupabase(propiedades, RestClient.builder());
    }
}
