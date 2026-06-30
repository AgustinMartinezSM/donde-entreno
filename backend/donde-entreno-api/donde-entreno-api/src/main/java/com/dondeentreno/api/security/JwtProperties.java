package com.dondeentreno.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Propiedades de JWT configurables por variables de entorno.
 */
@ConfigurationProperties(prefix = "dondeentreno.auth.jwt")
public class JwtProperties {

    static final int LONGITUD_MINIMA_SECRET = 32;
    private static final String ISSUER_DEFAULT = "dondeentreno-api";
    private static final long EXPIRACION_DEFAULT_MINUTOS = 60L;

    private String secret;
    private String issuer = ISSUER_DEFAULT;
    private Long expirationMinutes = EXPIRACION_DEFAULT_MINUTOS;

    public String getSecret() {
        return secret;
    }

    public String getIssuer() {
        if (issuer == null || issuer.isBlank()) {
            return ISSUER_DEFAULT;
        }

        return issuer.trim();
    }

    public Long getExpirationMinutes() {
        if (expirationMinutes == null) {
            return EXPIRACION_DEFAULT_MINUTOS;
        }

        return expirationMinutes;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setExpirationMinutes(Long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationSeconds() {
        return getExpirationMinutesValidado() * 60L;
    }

    public long getExpirationMinutesValidado() {
        long minutos = getExpirationMinutes();
        if (minutos <= 0) {
            throw new IllegalStateException("La expiracion JWT debe ser mayor a cero minutos.");
        }

        return minutos;
    }

    public SecretKey crearSecretKey() {
        String secretValidado = secret == null ? "" : secret.trim();
        if (secretValidado.length() < LONGITUD_MINIMA_SECRET) {
            throw new IllegalStateException("El secret JWT debe tener al menos 32 caracteres.");
        }

        return new SecretKeySpec(secretValidado.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
