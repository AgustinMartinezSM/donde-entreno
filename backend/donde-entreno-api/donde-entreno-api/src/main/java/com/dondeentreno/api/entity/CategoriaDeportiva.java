package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entidad que representa la tabla categoria_deportiva de PostgreSQL.
 *
 * Una categoría deportiva agrupa deportes relacionados.
 *
 * Ejemplos:
 * - Deportes de combate
 * - Fitness y entrenamiento
 * - Deportes de equipo
 * - Actividades acuáticas
 */
@Entity
@Table(name = "categoria_deportiva")
public class CategoriaDeportiva {

    /**
     * Identificador único de la categoría.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre visible de la categoría.
     *
     * Ejemplo:
     * "Deportes de combate"
     */
    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    /**
     * Slug usado para URLs amigables.
     *
     * Ejemplo:
     * "deportes-de-combate"
     */
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    /**
     * Descripción más larga de la categoría.
     *
     * En la base está definido como TEXT.
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * URL del ícono representativo de la categoría.
     */
    @Column(name = "icono_url", length = 255)
    private String iconoUrl;

    /**
     * Indica si la categoría está activa.
     *
     * En la base tiene default true.
     */
    @Column(name = "activa", nullable = false)
    private Boolean activa;

    /**
     * Orden visual para mostrar categorías en pantalla.
     *
     * En la base tiene default 0.
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
     *
     * JPA necesita poder crear objetos de esta clase automáticamente
     * cuando trae datos desde la base de datos.
     */
    public CategoriaDeportiva() {
    }

    public Long getId() {
        return id;
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

    public Boolean getActiva() {
        return activa;
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

    public void setActiva(Boolean activa) {
        this.activa = activa;
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