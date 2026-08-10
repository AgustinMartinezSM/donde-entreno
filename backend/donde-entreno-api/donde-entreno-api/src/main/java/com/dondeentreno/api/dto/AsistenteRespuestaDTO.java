package com.dondeentreno.api.dto;

import java.util.List;

/**
 * Respuesta del asistente.
 *
 * Misma forma que el tipo RespuestaAsistente del frontend
 * (lib/asistente/tipos.ts), para que la UI no tenga que cambiar: ya
 * sabe renderizar texto, enlaces como chips y opciones rápidas.
 *
 * "fuente" dice quién resolvió la consulta ("local" o "gemini"). Sirve
 * para medir cuánto aporta el modelo sin tener que loguear el texto del
 * usuario.
 */
public class AsistenteRespuestaDTO {

    private String texto;
    private List<AsistenteEnlaceDTO> enlaces;
    private List<String> opcionesRapidas;
    private String fuente;

    public AsistenteRespuestaDTO() {
    }

    public AsistenteRespuestaDTO(
            String texto,
            List<AsistenteEnlaceDTO> enlaces,
            List<String> opcionesRapidas,
            String fuente
    ) {
        this.texto = texto;
        this.enlaces = enlaces;
        this.opcionesRapidas = opcionesRapidas;
        this.fuente = fuente;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public List<AsistenteEnlaceDTO> getEnlaces() {
        return enlaces;
    }

    public void setEnlaces(List<AsistenteEnlaceDTO> enlaces) {
        this.enlaces = enlaces;
    }

    public List<String> getOpcionesRapidas() {
        return opcionesRapidas;
    }

    public void setOpcionesRapidas(List<String> opcionesRapidas) {
        this.opcionesRapidas = opcionesRapidas;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }
}
