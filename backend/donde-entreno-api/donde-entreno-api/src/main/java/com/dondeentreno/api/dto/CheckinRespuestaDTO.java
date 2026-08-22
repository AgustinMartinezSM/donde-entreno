package com.dondeentreno.api.dto;

/**
 * Respuesta de los endpoints de check-in (script 26).
 *
 * `registradoAhora` distingue el 201 (fila nueva) del 200 idempotente
 * (ya había check-in hoy); `yaRegistradoHoy` es lo que pinta el botón.
 */
public class CheckinRespuestaDTO {

    private boolean yaRegistradoHoy;
    private boolean registradoAhora;
    private long cantidadPersonasEntrenaron30Dias;

    public CheckinRespuestaDTO() {
    }

    public CheckinRespuestaDTO(
            boolean yaRegistradoHoy,
            boolean registradoAhora,
            long cantidadPersonasEntrenaron30Dias
    ) {
        this.yaRegistradoHoy = yaRegistradoHoy;
        this.registradoAhora = registradoAhora;
        this.cantidadPersonasEntrenaron30Dias = cantidadPersonasEntrenaron30Dias;
    }

    public boolean isYaRegistradoHoy() {
        return yaRegistradoHoy;
    }

    public void setYaRegistradoHoy(boolean yaRegistradoHoy) {
        this.yaRegistradoHoy = yaRegistradoHoy;
    }

    public boolean isRegistradoAhora() {
        return registradoAhora;
    }

    public void setRegistradoAhora(boolean registradoAhora) {
        this.registradoAhora = registradoAhora;
    }

    public long getCantidadPersonasEntrenaron30Dias() {
        return cantidadPersonasEntrenaron30Dias;
    }

    public void setCantidadPersonasEntrenaron30Dias(long cantidadPersonasEntrenaron30Dias) {
        this.cantidadPersonasEntrenaron30Dias = cantidadPersonasEntrenaron30Dias;
    }
}
