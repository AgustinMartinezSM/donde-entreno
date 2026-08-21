package com.dondeentreno.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request de cambio de password con sesion activa (fase 5a).
 */
public class CambiarPasswordRequestDTO {

    @NotBlank(message = "La password actual es obligatoria.")
    private String passwordActual;

    @NotBlank(message = "La password nueva es obligatoria.")
    private String passwordNueva;

    @NotBlank(message = "La confirmacion de password es obligatoria.")
    private String confirmarPassword;

    public CambiarPasswordRequestDTO() {
    }

    public CambiarPasswordRequestDTO(
            String passwordActual,
            String passwordNueva,
            String confirmarPassword
    ) {
        this.passwordActual = passwordActual;
        this.passwordNueva = passwordNueva;
        this.confirmarPassword = confirmarPassword;
    }

    public String getPasswordActual() {
        return passwordActual;
    }

    public String getPasswordNueva() {
        return passwordNueva;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public void setPasswordNueva(String passwordNueva) {
        this.passwordNueva = passwordNueva;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }
}
