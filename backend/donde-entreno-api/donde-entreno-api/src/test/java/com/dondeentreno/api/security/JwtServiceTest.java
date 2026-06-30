package com.dondeentreno.api.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET_TEST = "clave-ficticia-de-test-con-longitud-suficiente-123456";

    @Test
    void generaTokenConClaimsEsperadosYNoIncluyePasswordHash() {
        JwtProperties jwtProperties = crearProperties();
        JwtService jwtService = new JwtService(
                jwtEncoder(jwtProperties),
                jwtProperties,
                Clock.fixed(Instant.parse("2030-06-30T16:00:00Z"), ZoneOffset.UTC)
        );
        UsuarioPrincipal principal = new UsuarioPrincipal(
                1L,
                "admin@dondeentreno.com",
                "Admin",
                null,
                "SUPER_ADMIN",
                "$2a$10$hash-ficticio-no-real",
                true
        );

        String token = jwtService.generarAccessToken(principal);
        Jwt jwt = jwtDecoder(jwtProperties).decode(token);

        assertNotNull(token);
        assertEquals("admin@dondeentreno.com", jwt.getSubject());
        assertEquals("dondeentreno-api", jwt.getClaimAsString("iss"));
        assertEquals(Long.valueOf(1L), jwt.getClaim("userId"));
        assertEquals("SUPER_ADMIN", jwt.getClaim("rol"));
        assertEquals(List.of("SUPER_ADMIN"), jwt.getClaimAsStringList("roles"));
        assertEquals(Instant.parse("2030-06-30T16:00:00Z"), jwt.getIssuedAt());
        assertEquals(Instant.parse("2030-06-30T17:00:00Z"), jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
        assertFalse(jwt.getClaims().containsKey("passwordHash"));
    }

    @Test
    void secretCortoNoEsValido() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("secret-corto");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                jwtProperties::crearSecretKey
        );

        assertEquals("El secret JWT debe tener al menos 32 caracteres.", exception.getMessage());
    }

    private JwtProperties crearProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET_TEST);
        jwtProperties.setIssuer("dondeentreno-api");
        jwtProperties.setExpirationMinutes(60L);
        return jwtProperties;
    }

    private JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtProperties.crearSecretKey()));
    }

    private JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(
                        SECRET_TEST.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                ))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
