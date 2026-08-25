package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Hecho de un publicador en la línea de tiempo (script 32, Fase 6).
 *
 * Hasta ahora el "feed" era una consulta a `actividad`, así que solo
 * podía expresar UN tipo de hecho. Esta tabla lo vuelve un log: foto
 * nueva, actividad nueva, cambio aprobado.
 *
 * FKs planas (patrón de las entidades sociales): el DTO resuelve los
 * datos con queries agrupadas y no arrastra el grafo entero.
 */
@Entity
@Table(name = "feed_event")
public class FeedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "perfil_publicador_id", nullable = false)
    private Long perfilPublicadorId;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "imagen_id")
    private Long imagenId;

    /** La novedad del canal, cuando el hecho es una (script 34). */
    @Column(name = "novedad_id")
    private Long novedadId;

    /** El evento deportivo, cuando el hecho es uno (script 35). */
    @Column(name = "evento_deportivo_id")
    private Long eventoDeportivoId;

    /** Solo para lo que no se deduce del join (ej: "3 fotos nuevas"). */
    @Column(name = "resumen", length = 200)
    private String resumen;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public FeedEvent() {
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public Long getNovedadId() {
        return novedadId;
    }

    public void setNovedadId(Long novedadId) {
        this.novedadId = novedadId;
    }

    public Long getEventoDeportivoId() {
        return eventoDeportivoId;
    }

    public void setEventoDeportivoId(Long eventoDeportivoId) {
        this.eventoDeportivoId = eventoDeportivoId;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
