package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Un mensaje dentro de una conversación (script 36).
 *
 * `autor` es un ROL y no un usuario_id: del lado del publicador quien
 * responde es el perfil, no la persona, así que el hilo no cambia de
 * firma si mañana ese perfil lo maneja otra cuenta.
 */
@Entity
@Table(name = "mensaje")
public class Mensaje {

    public static final String AUTOR_USUARIO = "USUARIO";
    public static final String AUTOR_PUBLICADOR = "PUBLICADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversacion_id", nullable = false)
    private Long conversacionId;

    @Column(name = "autor", nullable = false, length = 20)
    private String autor;

    @Column(name = "texto", nullable = false, length = 2000)
    private String texto;

    /**
     * Se guarda para el contador de no leídos. La HORA no se expone al
     * otro lado: un "visto a las 14:32" crea una expectativa de
     * respuesta inmediata que un club no puede cumplir.
     */
    @Column(name = "leido_at")
    private OffsetDateTime leidoAt;

    /** VISIBLE | OCULTO_POR_ADMIN. */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Mensaje() {
    }

    public Long getId() {
        return id;
    }

    public Long getConversacionId() {
        return conversacionId;
    }

    public void setConversacionId(Long conversacionId) {
        this.conversacionId = conversacionId;
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

    public OffsetDateTime getLeidoAt() {
        return leidoAt;
    }

    public void setLeidoAt(OffsetDateTime leidoAt) {
        this.leidoAt = leidoAt;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
