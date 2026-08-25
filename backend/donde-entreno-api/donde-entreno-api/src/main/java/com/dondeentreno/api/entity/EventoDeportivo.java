package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Un evento deportivo: lo que pasa UNA vez y tiene fecha (script 35,
 * Fase 9). Un torneo, una clase abierta, un seminario.
 *
 * Se llama `EventoDeportivo` y no `Evento` porque en este código ya
 * conviven `EventoInteraccion` (tracking de clicks) y `FeedEvent` (el
 * log del feed): un `Evento` a secas obligaría a mirar el import cada
 * vez que se lee una línea.
 *
 * Lo que se repite todas las semanas NO va acá: eso es una actividad
 * con sus `horario_actividad`.
 *
 * FKs planas (patrón de las entidades sociales): el service resuelve
 * lo que necesita con queries batch.
 */
@Entity
@Table(name = "evento_deportivo")
public class EventoDeportivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_publicador_id", nullable = false)
    private Long perfilPublicadorId;

    /** Opcional: el torneo del club no cuelga de ninguna actividad. */
    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "deporte_id", nullable = false)
    private Long deporteId;

    /** Una sede del publicador: de acá salen ciudad, barrio y punto. */
    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    /** Una foto YA publicada del publicador. Opcional. */
    @Column(name = "imagen_id")
    private Long imagenId;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "inicia_at", nullable = false)
    private OffsetDateTime iniciaAt;

    @Column(name = "termina_at")
    private OffsetDateTime terminaAt;

    /** Informativo: no hay reserva de lugares en V1. */
    @Column(name = "cupo")
    private Integer cupo;

    @Column(name = "es_gratis", nullable = false)
    private Boolean esGratis;

    @Column(name = "precio_referencia")
    private BigDecimal precioReferencia;

    @Column(name = "mostrar_precio", nullable = false)
    private Boolean mostrarPrecio;

    /** PUBLICADO | CANCELADO | OCULTO_POR_ADMIN | ELIMINADO_POR_PUBLICADOR. */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public EventoDeportivo() {
    }

    public Long getId() {
        return id;
    }

    public Long getPerfilPublicadorId() {
        return perfilPublicadorId;
    }

    public void setPerfilPublicadorId(Long perfilPublicadorId) {
        this.perfilPublicadorId = perfilPublicadorId;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public Long getDeporteId() {
        return deporteId;
    }

    public void setDeporteId(Long deporteId) {
        this.deporteId = deporteId;
    }

    public Long getUbicacionId() {
        return ubicacionId;
    }

    public void setUbicacionId(Long ubicacionId) {
        this.ubicacionId = ubicacionId;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getIniciaAt() {
        return iniciaAt;
    }

    public void setIniciaAt(OffsetDateTime iniciaAt) {
        this.iniciaAt = iniciaAt;
    }

    public OffsetDateTime getTerminaAt() {
        return terminaAt;
    }

    public void setTerminaAt(OffsetDateTime terminaAt) {
        this.terminaAt = terminaAt;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public Boolean getEsGratis() {
        return esGratis;
    }

    public void setEsGratis(Boolean esGratis) {
        this.esGratis = esGratis;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
