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
 * Entidad que representa la tabla imagen de PostgreSQL.
 *
 * Guarda imágenes relacionadas a perfiles publicadores o actividades.
 *
 * Ejemplos:
 * - Logo de un club
 * - Portada de un gimnasio
 * - Imagen principal de una actividad
 * - Galería de fotos de una clase
 *
 * Importante:
 * No guardamos el archivo binario dentro de PostgreSQL.
 * Guardamos solamente la URL o ruta del archivo.
 */
@Entity
@Table(name = "imagen")
public class Imagen {

    /**
     * Identificador único de la imagen.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Perfil publicador dueño de la imagen.
     *
     * Esta relación es opcional.
     *
     * En la tabla imagen existe la columna:
     * perfil_publicador_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_publicador_id")
    private PerfilPublicador perfilPublicador;

    /**
     * Actividad dueña de la imagen.
     *
     * Esta relación es opcional.
     *
     * En la tabla imagen existe la columna:
     * actividad_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /**
     * URL o ruta de la imagen.
     *
     * Ejemplo:
     * https://...
     * /uploads/actividades/boxeo.jpg
     */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /**
     * Tipo de imagen.
     *
     * Valores permitidos en base:
     * LOGO, PORTADA, PRINCIPAL, GALERIA.
     *
     * Por ahora lo manejamos como String para avanzar simple.
     * Más adelante podemos convertirlo a enum.
     */
    @Column(name = "tipo_imagen", nullable = false, length = 50)
    private String tipoImagen;

    /**
     * Título opcional de la imagen.
     */
    @Column(name = "titulo", length = 150)
    private String titulo;

    /**
     * Descripción opcional de la imagen.
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /**
     * Orden visual para mostrar imágenes.
     */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    /**
     * Indica si la imagen está activa.
     */
    @Column(name = "activa", nullable = false)
    private Boolean activa;

    /**
     * Fecha de creación.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Fecha de actualización.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Constructor vacío obligatorio para JPA.
     */
    public Imagen() {
    }

    public Long getId() {
        return id;
    }

    public PerfilPublicador getPerfilPublicador() {
        return perfilPublicador;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public String getUrl() {
        return url;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getOrden() {
        return orden;
    }

    public Boolean getActiva() {
        return activa;
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

    public void setPerfilPublicador(PerfilPublicador perfilPublicador) {
        this.perfilPublicador = perfilPublicador;
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
