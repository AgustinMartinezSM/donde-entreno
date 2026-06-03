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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad que representa la tabla ubicacion de PostgreSQL.
 *
 * Guarda los lugares físicos donde se realizan las actividades.
 *
 * Ejemplos:
 * - Sede principal de un club
 * - Gimnasio
 * - Plaza
 * - Polideportivo
 * - Estudio privado
 *
 * Una ubicación pertenece a:
 * - un perfil publicador
 * - una ciudad
 * - un barrio o zona
 */
@Entity
@Table(name = "ubicacion")
public class Ubicacion {

    /**
     * Identificador único de la ubicación.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Perfil publicador dueño de esta ubicación.
     *
     * En la tabla ubicacion existe la columna:
     * perfil_publicador_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_publicador_id", nullable = false)
    private PerfilPublicador perfilPublicador;

    /**
     * Ciudad donde se encuentra la ubicación.
     *
     * En la tabla ubicacion existe la columna:
     * ciudad_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    /**
     * Barrio o zona donde se encuentra la ubicación.
     *
     * En la tabla ubicacion existe la columna:
     * barrio_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id", nullable = false)
    private Barrio barrio;

    /**
     * Nombre de la ubicación.
     *
     * Ejemplo:
     * "Sede principal", "Gimnasio central", "Plaza Mitre".
     */
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /**
     * Dirección física de la ubicación.
     */
    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    /**
     * Referencia opcional para ayudar a encontrar el lugar.
     *
     * Ejemplo:
     * "Entrada por calle lateral".
     */
    @Column(name = "referencia", length = 255)
    private String referencia;

    /**
     * Latitud geográfica.
     *
     * En PostgreSQL está definido como NUMERIC(9,6).
     * Por eso usamos BigDecimal en Java.
     */
    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal latitud;

    /**
     * Longitud geográfica.
     *
     * En PostgreSQL está definido como NUMERIC(9,6).
     * Por eso usamos BigDecimal en Java.
     */
    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal longitud;

    /**
     * URL de Google Maps.
     */
    @Column(name = "google_maps_url", length = 500)
    private String googleMapsUrl;

    /**
     * Indica si la ubicación está activa.
     *
     * En la tabla se llama activa.
     */
    @Column(name = "activa", nullable = false)
    private Boolean activa;

    /**
     * Fecha de borrado lógico.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

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
    public Ubicacion() {
    }

    public Long getId() {
        return id;
    }

    public PerfilPublicador getPerfilPublicador() {
        return perfilPublicador;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }

    public Barrio getBarrio() {
        return barrio;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public Boolean getActiva() {
        return activa;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
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

    public void setCiudad(Ciudad ciudad) {
        this.ciudad = ciudad;
    }

    public void setBarrio(Barrio barrio) {
        this.barrio = barrio;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
