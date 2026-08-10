package com.dondeentreno.api.dto;

/**
 * Enlace interno que ofrece el asistente.
 *
 * Siempre es una ruta relativa de la propia app, armada por el backend a
 * partir de slugs validados contra la base. Nunca la escribe un modelo.
 */
public class AsistenteEnlaceDTO {

    private String href;
    private String etiqueta;

    public AsistenteEnlaceDTO() {
    }

    public AsistenteEnlaceDTO(String href, String etiqueta) {
        this.href = href;
        this.etiqueta = etiqueta;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
