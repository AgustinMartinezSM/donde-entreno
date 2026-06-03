package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entidad que representa la tabla deporte de PostgreSQL.
 *
 * Un deporte es una disciplina concreta dentro de DondeEntreno.
 *
 * Ejemplos:
 * - Boxeo
 * - Jiu Jitsu
 * - Fútbol
 * - Yoga
 * - Natación
 *
 * Cada deporte pertenece a una categoría deportiva.
 */
@Entity
@Table(name = "deporte")
public class Deporte {

    /**
     * Identificador único del deporte.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Categoría deportiva a la que pertenece este deporte.
     *
     * En la tabla deporte existe la columna:
     * categoria_deportiva_id
     *
     * Esa columna apunta al id de la tabla categoria_deportiva.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_deportiva_id", nullable = false)
    private CategoriaDeportiva categoriaDeportiva;

    /**
     * Nombre visible del deporte.
     *
     * Ejemplo:
     * "Boxeo"
     */
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Slug usado para URLs amigables.
     *
     * Ejemplo:
     * "boxeo"
     */
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    /**
     * Descripción del deporte.
     *
     * En la base está definido como TEXT.
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * URL del ícono representativo del deporte.
     */
    @Column(name = "icono_url", length = 255)
    private String iconoUrl;

    /**
     * Indica si el deporte está activo.
     *
     * En la tabla se llama activo, no activa.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    /**
     * Orden visual para mostrar deportes en pantalla.
     */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    /**
     * Fecha y hora de creación del registro.
     *
     * En PostgreSQL corresponde a TIMESTAMPTZ.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Fecha y hora de última actualización del registro.
     *
     * En PostgreSQL corresponde a TIMESTAMPTZ.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Constructor vacío obligatorio para JPA.
     */
    public Deporte() {
    }

    public Long getId() {
        return id;
    }

    public CategoriaDeportiva getCategoriaDeportiva() {
        return categoriaDeportiva;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIconoUrl() {
        return iconoUrl;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Integer getOrden() {
        return orden;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCategoriaDeportiva(CategoriaDeportiva categoriaDeportiva) {
        this.categoriaDeportiva = categoriaDeportiva;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setIconoUrl(String iconoUrl) {
        this.iconoUrl = iconoUrl;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}