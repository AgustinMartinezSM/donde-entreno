package com.dondeentreno.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Una conversación lista para pintar en la bandeja (inbox).
 *
 * `contraparte*` es "el otro lado" según quién mira: para el usuario
 * es el publicador; para el publicador, el usuario con nombre
 * abreviado ("Ana G."), igual que en los comentarios de la Fase 4.
 * Su email y su apellido completo NO viajan: nadie los necesita para
 * responder una consulta.
 */
public class ConversacionDTO {

    private Long id;
    private String estado;
    private OffsetDateTime ultimoMensajeAt;
    private Long noLeidos;

    private String contraparteNombre;
    private String contraparteLogoUrl;
    /** Solo cuando la contraparte es un publicador. */
    private Long perfilPublicadorId;
    private String perfilSlug;

    private Long actividadId;
    private String actividadTitulo;
    private String actividadSlug;

    /** Arranque del último mensaje, para la vista previa. */
    private String ultimoMensajeTexto;

    /** Solo al abrir el hilo. */
    private List<MensajeDTO> mensajes;

    public ConversacionDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getUltimoMensajeAt() {
        return ultimoMensajeAt;
    }

    public void setUltimoMensajeAt(OffsetDateTime ultimoMensajeAt) {
        this.ultimoMensajeAt = ultimoMensajeAt;
    }

    public Long getNoLeidos() {
        return noLeidos;
    }

    public void setNoLeidos(Long noLeidos) {
        this.noLeidos = noLeidos;
    }

    public String getContraparteNombre() {
        return contraparteNombre;
    }

    public void setContraparteNombre(String contraparteNombre) {
        this.contraparteNombre = contraparteNombre;
    }

    public String getContraparteLogoUrl() {
        return contraparteLogoUrl;
    }

    public void setContraparteLogoUrl(String contraparteLogoUrl) {
        this.contraparteLogoUrl = contraparteLogoUrl;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public String getPerfilSlug() {
        return perfilSlug;
    }

    public void setPerfilSlug(String perfilSlug) {
        this.perfilSlug = perfilSlug;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public String getActividadTitulo() {
        return actividadTitulo;
    }

    public void setActividadTitulo(String actividadTitulo) {
        this.actividadTitulo = actividadTitulo;
    }

    public String getActividadSlug() {
        return actividadSlug;
    }

    public void setActividadSlug(String actividadSlug) {
        this.actividadSlug = actividadSlug;
    }

    public String getUltimoMensajeTexto() {
        return ultimoMensajeTexto;
    }

    public void setUltimoMensajeTexto(String ultimoMensajeTexto) {
        this.ultimoMensajeTexto = ultimoMensajeTexto;
    }

    public List<MensajeDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<MensajeDTO> mensajes) {
        this.mensajes = mensajes;
    }
}
