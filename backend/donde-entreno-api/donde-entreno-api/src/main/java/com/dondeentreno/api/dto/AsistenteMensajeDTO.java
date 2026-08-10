package com.dondeentreno.api.dto;

/**
 * Un mensaje del historial reciente de la conversación.
 *
 * Lo manda el frontend en cada consulta: no se guarda nada en la base ni
 * en memoria del servidor. La charla vive en la pestaña del usuario y se
 * pierde al cerrarla, que es exactamente lo que queremos de algo que puede
 * salir hacia un modelo externo.
 *
 * "autor" usa las mismas dos palabras que el frontend ("usuario" /
 * "asistente") para no traducir nada en el medio.
 */
public class AsistenteMensajeDTO {

    private String autor;
    private String texto;

    public AsistenteMensajeDTO() {
    }

    public AsistenteMensajeDTO(String autor, String texto) {
        this.autor = autor;
        this.texto = texto;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean esDelUsuario() {
        return "usuario".equalsIgnoreCase(autor);
    }

    public boolean esDelAsistente() {
        return "asistente".equalsIgnoreCase(autor);
    }
}
