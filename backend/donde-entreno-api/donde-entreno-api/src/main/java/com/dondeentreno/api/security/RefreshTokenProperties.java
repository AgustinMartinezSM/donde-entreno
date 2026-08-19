package com.dondeentreno.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del refresh token, configurables por variables de entorno.
 *
 * El default (30 dias) alcanza para produccion: sin configurar nada en
 * Render, funciona. La ventana es deslizante: cada rotacion emite un
 * token nuevo con 30 dias frescos, sin tope duro por familia (decision
 * de producto del 2026-08-18).
 */
@ConfigurationProperties(prefix = "dondeentreno.auth.refresh")
public class RefreshTokenProperties {

    private static final long EXPIRACION_DEFAULT_DIAS = 30L;

    private Long expirationDays = EXPIRACION_DEFAULT_DIAS;

    public Long getExpirationDays() {
        if (expirationDays == null) {
            return EXPIRACION_DEFAULT_DIAS;
        }

        return expirationDays;
    }

    public void setExpirationDays(Long expirationDays) {
        this.expirationDays = expirationDays;
    }

    public long getExpirationDaysValidado() {
        long dias = getExpirationDays();
        if (dias <= 0) {
            throw new IllegalStateException("La expiracion del refresh token debe ser mayor a cero dias.");
        }

        return dias;
    }

    public long getExpirationSeconds() {
        return getExpirationDaysValidado() * 24L * 60L * 60L;
    }
}
