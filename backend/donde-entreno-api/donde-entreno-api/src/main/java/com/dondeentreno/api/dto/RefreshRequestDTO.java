package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request de POST /api/auth/refresh y POST /api/auth/logout: el refresh
 * token en claro que el cliente guarda.
 */
public class RefreshRequestDTO {

    @NotBlank(message = "El refresh token es obligatorio.")
    private String refreshToken;

    public RefreshRequestDTO() {
    }

    public RefreshRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
