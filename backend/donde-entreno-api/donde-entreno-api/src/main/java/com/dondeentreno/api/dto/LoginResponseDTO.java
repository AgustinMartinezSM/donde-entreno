package com.dondeentreno.api.dto;

/**
 * Response exitosa de login JWT.
 */
public class LoginResponseDTO {

    private String tokenType;
    private String accessToken;
    private long expiresIn;
    private AuthUsuarioDTO usuario;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String tokenType, String accessToken, long expiresIn, AuthUsuarioDTO usuario) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.usuario = usuario;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public AuthUsuarioDTO getUsuario() {
        return usuario;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setUsuario(AuthUsuarioDTO usuario) {
        this.usuario = usuario;
    }
}
