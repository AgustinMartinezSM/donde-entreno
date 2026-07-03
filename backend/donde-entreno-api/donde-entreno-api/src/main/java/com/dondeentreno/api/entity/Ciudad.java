package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entidad que representa la tabla ciudad de PostgreSQL.
 *
 * Guarda las ciudades donde DondeEntreno tendrá actividades.
 *
 * Ejemplo:
 * - Mar del Plata
 * - Miramar
 * - Villa Gesell
 *
 * Aunque el MVP arranque con una ciudad, esta entidad
 * deja el backend preparado para crecer a otras ciudades.
 */
@Entity
@Table(name = "ciudad")
public class Ciudad {

    /**
     * Identificador único de la ciudad.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la ciudad.
     *
     * Ejemplo:
     * "Mar del Plata"
     */
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Provincia donde se encuentra la ciudad.
     *
     * Ejemplo:
     * "Buenos Aires"
     */
    @Column(name = "provincia", nullable = false, length = 100)
    private String provincia;

    /**
     * País donde se encuentra la ciudad.
     *
     * Ejemplo:
     * "Argentina"
     */
    @Column(name = "pais", nullable = false, length = 100)
    private String pais;

    /**
     * Slug publico usado para rutas territoriales.
     *
     * Ejemplo:
     * "mar-del-plata"
     */
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    /**
     * Orden editorial para listar ciudades activas.
     *
     * Los valores mas bajos aparecen primero.
     */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    /**
     * Indica si la ciudad está activa.
     *
     * En la base tiene default true.
     */
    @Column(name = "activa", nullable = false)
    private Boolean activa;

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
     * JPA lo necesita para crear objetos Ciudad automáticamente
     * cuando lee datos desde PostgreSQL.
     */
    public Ciudad() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getPais() {
        return pais;
    }

    public String getSlug() {
        return slug;
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

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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
