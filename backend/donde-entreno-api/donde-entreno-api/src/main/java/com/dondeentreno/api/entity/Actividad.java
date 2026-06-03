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
 * Entidad que representa la tabla actividad de PostgreSQL.
 *
 * Esta es una de las entidades principales de DondeEntreno.
 *
 * Una actividad representa una propuesta deportiva concreta
 * que el visitante puede buscar y consultar.
 *
 * Ejemplos:
 * - Boxeo recreativo para adultos
 * - Jiu Jitsu para principiantes
 * - Fútbol infantil categoría 2014
 * - Yoga inicial
 * - Funcional femenino
 *
 * Una actividad pertenece a:
 * - un perfil publicador
 * - un deporte
 * - una ubicación
 */
@Entity
@Table(name = "actividad")
public class Actividad {

    /**
     * Identificador único de la actividad.
     *
     * En PostgreSQL corresponde a:
     * id BIGSERIAL PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Perfil publicador que ofrece esta actividad.
     *
     * En la tabla actividad existe la columna:
     * perfil_publicador_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_publicador_id", nullable = false)
    private PerfilPublicador perfilPublicador;

    /**
     * Deporte principal de la actividad.
     *
     * En la tabla actividad existe la columna:
     * deporte_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deporte_id", nullable = false)
    private Deporte deporte;

    /**
     * Ubicación donde se realiza la actividad.
     *
     * En la tabla actividad existe la columna:
     * ubicacion_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    /**
     * Título visible de la actividad.
     *
     * Ejemplo:
     * "Boxeo recreativo para adultos"
     */
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    /**
     * Slug usado para URLs amigables.
     *
     * Ejemplo:
     * "boxeo-recreativo-para-adultos"
     */
    @Column(name = "slug", nullable = false, unique = true, length = 180)
    private String slug;

    /**
     * Descripción completa de la actividad.
     *
     * En PostgreSQL está definido como TEXT.
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Edad mínima recomendada o permitida.
     */
    @Column(name = "edad_minima")
    private Integer edadMinima;

    /**
     * Edad máxima recomendada o permitida.
     */
    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    /**
     * Nivel de la actividad.
     *
     * Valores permitidos en base:
     * PRINCIPIANTE, INTERMEDIO, AVANZADO, TODOS.
     *
     * Por ahora lo manejamos como String para avanzar simple.
     * Más adelante podemos convertirlo a enum.
     */
    @Column(name = "nivel", nullable = false, length = 50)
    private String nivel;

    /**
     * Enfoque de la actividad.
     *
     * Valores permitidos:
     * RECREATIVO, COMPETITIVO, MIXTO.
     */
    @Column(name = "enfoque", nullable = false, length = 50)
    private String enfoque;

    /**
     * Modalidad de la actividad.
     *
     * Valores permitidos:
     * PRESENCIAL, ONLINE, MIXTA.
     */
    @Column(name = "modalidad", nullable = false, length = 50)
    private String modalidad;

    /**
     * Precio de referencia de la actividad.
     *
     * En PostgreSQL está definido como NUMERIC(10,2).
     */
    @Column(name = "precio_referencia", precision = 10, scale = 2)
    private BigDecimal precioReferencia;

    /**
     * Indica si el precio debe mostrarse públicamente.
     */
    @Column(name = "mostrar_precio", nullable = false)
    private Boolean mostrarPrecio;

    /**
     * Indica si requiere inscripción previa.
     */
    @Column(name = "requiere_inscripcion", nullable = false)
    private Boolean requiereInscripcion;

    /**
     * Indica si los cupos son limitados.
     */
    @Column(name = "cupos_limitados", nullable = false)
    private Boolean cuposLimitados;

    /**
     * WhatsApp específico de contacto para esta actividad.
     */
    @Column(name = "whatsapp_contacto", length = 30)
    private String whatsappContacto;

    /**
     * Instagram específico de contacto para esta actividad.
     */
    @Column(name = "instagram_contacto", length = 150)
    private String instagramContacto;

    /**
     * Email específico de contacto para esta actividad.
     */
    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    /**
     * Estado de publicación de la actividad.
     *
     * Valores permitidos:
     * BORRADOR, PENDIENTE_REVISION, PUBLICADA, PAUSADA, RECHAZADA.
     */
    @Column(name = "estado_publicacion", nullable = false, length = 50)
    private String estadoPublicacion;

    /**
     * Motivo de rechazo, si un admin rechaza la publicación.
     */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    /**
     * Indica si la actividad está activa.
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
    public Actividad() {
    }

    public Long getId() {
        return id;
    }

    public PerfilPublicador getPerfilPublicador() {
        return perfilPublicador;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getEdadMinima() {
        return edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public String getNivel() {
        return nivel;
    }

    public String getEnfoque() {
        return enfoque;
    }

    public String getModalidad() {
        return modalidad;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public Boolean getRequiereInscripcion() {
        return requiereInscripcion;
    }

    public Boolean getCuposLimitados() {
        return cuposLimitados;
    }

    public String getWhatsappContacto() {
        return whatsappContacto;
    }

    public String getInstagramContacto() {
        return instagramContacto;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
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

    public void setDeporte(Deporte deporte) {
        this.deporte = deporte;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setEdadMinima(Integer edadMinima) {
        this.edadMinima = edadMinima;
    }

    public void setEdadMaxima(Integer edadMaxima) {
        this.edadMaxima = edadMaxima;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public void setEnfoque(String enfoque) {
        this.enfoque = enfoque;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
    }

    public void setRequiereInscripcion(Boolean requiereInscripcion) {
        this.requiereInscripcion = requiereInscripcion;
    }

    public void setCuposLimitados(Boolean cuposLimitados) {
        this.cuposLimitados = cuposLimitados;
    }

    public void setWhatsappContacto(String whatsappContacto) {
        this.whatsappContacto = whatsappContacto;
    }

    public void setInstagramContacto(String instagramContacto) {
        this.instagramContacto = instagramContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public void setEstadoPublicacion(String estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
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
