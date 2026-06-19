package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO para respuestas de error.
 *
 * Este objeto define el formato que va a devolver la API
 * cuando ocurra un error controlado.
 */
public class ErrorResponseDTO {

    private int status;
    private String error;
    private String mensaje;
    private Map<String, String> errores;
    private String path;
    private OffsetDateTime timestamp;

    public ErrorResponseDTO() {
    }

    public ErrorResponseDTO(
            int status,
            String error,
            String mensaje,
            String path,
            OffsetDateTime timestamp
    ) {
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.path = path;
        this.timestamp = timestamp;
    }

    public ErrorResponseDTO(
            int status,
            String error,
            String mensaje,
            Map<String, String> errores,
            String path,
            OffsetDateTime timestamp
    ) {
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.errores = errores;
        this.path = path;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Map<String, String> getErrores() {
        return errores;
    }

    public String getPath() {
        return path;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setErrores(Map<String, String> errores) {
        this.errores = errores;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
