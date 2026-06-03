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
 * Entidad que representa la tabla usuario de PostgreSQL.
 *
 * Por ahora la usamos solo para poder relacionar correctamente
 * otras entidades, como PerfilPublicador.
 *
 * Más adelante vamos a volver a esta clase cuando trabajemos
 * login, roles, Spring Security y JWT.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    /**
     * Identificador único del usuario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Rol del usuario.
     *
     * En la tabla usuario existe la columna rol_id.
     * Esta relación apunta a la tabla rol.
     *
     * Todavía no vamos a trabajar permisos ni login,
     * pero dejamos la relación preparada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    /**
     * Nombre del usuario.
     */
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Apellido del usuario.
     */
    @Column(name = "apellido", length = 100)
    private String apellido;

    /**
     * Email único del usuario.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Hash de la contraseña.
     *
     * Nunca guardamos contraseñas en texto plano.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Teléfono opcional.
     */
    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Indica si el usuario está activo.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    /**
     * Indica si el email fue verificado.
     */
    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado;

    /**
     * Fecha del último inicio de sesión.
     */
    @Column(name = "ultimo_login_at")
    private OffsetDateTime ultimoLoginAt;

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
    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getTelefono() {
        return telefono;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public OffsetDateTime getUltimoLoginAt() {
        return ultimoLoginAt;
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

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public void setUltimoLoginAt(OffsetDateTime ultimoLoginAt) {
        this.ultimoLoginAt = ultimoLoginAt;
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