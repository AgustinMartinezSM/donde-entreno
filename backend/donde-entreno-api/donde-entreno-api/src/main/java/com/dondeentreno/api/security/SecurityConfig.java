package com.dondeentreno.api.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion base de seguridad para la API.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
        SuperAdminBootstrapProperties.class,
        JwtProperties.class,
        RefreshTokenProperties.class
})
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/actividades", "/api/actividades/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/barrios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categorias-deportivas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ciudades", "/api/ciudades/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/deportes").permitAll()
                        /* Fase 6: ranking público de lo más visto. */
                        .requestMatchers(HttpMethod.GET, "/api/deportes/populares").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/filtros/opciones").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/perfiles-publicadores",
                                "/api/perfiles-publicadores/**"
                        ).permitAll()
                        /*
                          Calendario de eventos (Fase 9): publico como el
                          catalogo. El JWT es OPCIONAL — con sesion cada
                          evento sabe si el usuario marco "me interesa".
                        */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/eventos",
                                "/api/eventos/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ubicaciones").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes-publicacion").permitAll()
                        /*
                          Asistente publico: no requiere cuenta, igual que
                          el resto del descubrimiento. La proteccion contra
                          abuso no es la autenticacion sino el limite por IP
                          del controller y el tope diario del asistente.
                        */
                        .requestMatchers(HttpMethod.POST, "/api/asistente/consulta").permitAll()
                        /*
                          Tracking anónimo de interacciones (Fase 2 social):
                          público a propósito — los visitantes también
                          cuentan. Rate limit por IP en el controller.
                        */
                        .requestMatchers(HttpMethod.POST, "/api/actividades/*/interacciones").permitAll()
                        /* Comentarios de fotos (Fase 4): leer es público. */
                        .requestMatchers(HttpMethod.GET, "/api/imagenes/*/comentarios").permitAll()
                        /* Fase 5: el click de contacto desde el perfil. */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/perfiles-publicadores/*/interacciones"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        /*
                          Refresh y logout son publicos como el login: la
                          credencial es el refresh token del body (256
                          bits aleatorios), no el access token — que
                          puede estar vencido justo cuando mas se
                          necesita refrescar.
                        */
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro/usuario").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro/publicador").permitAll()
                        /*
                          Explicito a proposito: es el unico endpoint de
                          /api/auth que exige sesion (fase 5a). Que no se
                          sume por accidente a la lista de permitAll de
                          arriba.
                        */
                        .requestMatchers(HttpMethod.POST, "/api/auth/cambiar-password").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/publicador/**").hasRole("PUBLICADOR")
                        /*
                          Explicita y no via anyRequest(): el espacio del
                          usuario comun (favoritos, deportes, seguimientos,
                          feed) requiere sesion pero NINGUN rol especial —
                          que quede escrito y no dependa del fallback.
                          (Deuda registrada desde la capa social; saldada
                          con el bloque de sync, que es su "plan previo".)
                        */
                        .requestMatchers("/api/usuario/**").authenticated()
                        .anyRequest().authenticated()
                )
                .build();
    }

    /**
     * Configuracion unica de CORS para toda la API.
     *
     * Este bean es el unico mecanismo CORS de la aplicacion:
     * Spring Security lo detecta por su nombre (corsConfigurationSource)
     * a traves de cors(Customizer.withDefaults()) en el filter chain.
     *
     * Antes existia ademas un WebMvcConfigurer con addCorsMappings,
     * lo que duplicaba la configuracion en dos mecanismos distintos.
     *
     * Los origenes permitidos se leen de la property
     * app.cors.allowed-origins (lista separada por comas).
     *
     * Ejemplo local:
     * Frontend Next.js: http://localhost:3000
     * Backend Spring Boot: http://localhost:8080
     *
     * @param allowedOrigins origenes permitidos para consumir la API.
     * @return configuracion CORS aplicada a las rutas /api/**.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String[] allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRolesGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtProperties.crearSecretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(jwtProperties.crearSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer()));
        return jwtDecoder;
    }
}
