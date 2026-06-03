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
 * Entidad que representa la tabla barrio de PostgreSQL.
 *
 * Guarda barrios o zonas dentro de una ciudad.
 *
 * Ejemplos:
 * - Centro
 * - La Perla
 * - Güemes
 * - Puerto
 * - Constitución
 *
 * Cada barrio pertenece a una ciudad.
 */
@Entity
@Table(name = "barrio")
public class Barrio {

    /**
     * Identificador único del barrio.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ciudad a la que pertenece este barrio.
     *
     * En la tabla barrio existe la columna:
     * ciudad_id
     *
     * Esa columna apunta al id de la tabla ciudad.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    /**
     * Nombre del barrio o zona.
     *
     * Ejemplo:
     * "Centro"
     */
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Indica si el barrio está activo.
     *
     * En la tabla se llama activo.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo;

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
    public Barrio() {
    }

    public Long getId() {
        return id;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }

    public String getNombre() {
        return nombre;
    }

    public Boolean getActivo() {
        return activo;
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

    public void setCiudad(Ciudad ciudad) {
        this.ciudad = ciudad;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}