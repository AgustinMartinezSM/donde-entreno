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
 * Entidad que representa la tabla perfil_publicador de PostgreSQL.
 *
 * Esta tabla guarda la información pública visible de quien publica actividades:
 * - Club
 * - Gimnasio
 * - Profesor independiente
 * - Institución
 * - Escuela deportiva
 * - Espacio de entrenamiento
 *
 * Importante:
 * usuario = cuenta de acceso/login.
 * perfilPublicador = perfil público visible en la plataforma.
 */
@Entity
@Table(name = "perfil_publicador")
public class PerfilPublicador {

    /**
     * Identificador único del perfil publicador.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario dueño de este perfil publicador.
     *
     * En la tabla perfil_publicador existe la columna usuario_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Nombre público del club, gimnasio, profesor o institución.
     */
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /**
     * Tipo de publicador.
     *
     * Valores permitidos en base:
     * CLUB, GIMNASIO, PROFESOR_INDEPENDIENTE,
     * INSTITUCION, ESCUELA_DEPORTIVA, ESPACIO_ENTRENAMIENTO.
     *
     * Por ahora lo manejamos como String para avanzar simple.
     * Más adelante podemos convertirlo a enum.
     */
    @Column(name = "tipo_publicador", nullable = false, length = 50)
    private String tipoPublicador;

    /**
     * Descripción pública del perfil.
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Email público de contacto.
     */
    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    /**
     * Teléfono público de contacto.
     */
    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto;

    /**
     * WhatsApp público de contacto.
     */
    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    /**
     * Instagram del perfil.
     */
    @Column(name = "instagram", length = 150)
    private String instagram;

    /**
     * Sitio web del perfil.
     */
    @Column(name = "sitio_web", length = 255)
    private String sitioWeb;

    /**
     * Indica si el perfil está activo.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    /**
     * Indica si el perfil fue verificado por DondeEntreno.
     */
    @Column(name = "verificado", nullable = false)
    private Boolean verificado;

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
    public PerfilPublicador() {
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Boolean getVerificado() {
        return verificado;
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

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setSitioWeb(String sitioWeb) {
        this.sitioWeb = sitioWeb;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setVerificado(Boolean verificado) {
        this.verificado = verificado;
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