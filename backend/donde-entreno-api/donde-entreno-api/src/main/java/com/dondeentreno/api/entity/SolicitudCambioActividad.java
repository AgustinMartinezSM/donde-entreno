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
 * Solicitud de cambio sobre una actividad ya publicada.
 *
 * Soporta el flujo de edicion con revision: el publicador propone
 * valores nuevos y la actividad publica no cambia hasta que un admin
 * aprueba. Cada campo de datos propuesto en null significa
 * "sin cambio propuesto" para ese campo.
 *
 * La tabla funciona ademas como historial de cambios: las solicitudes
 * aprobadas o rechazadas no se borran.
 *
 * Ver database/scripts/14_create_solicitud_cambio_actividad.sql
 * y docs/plan-solicitud-cambio-actividad.md
 */
@Entity
@Table(name = "solicitud_cambio_actividad")
public class SolicitudCambioActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Actividad publicada sobre la que se piden los cambios.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    /**
     * Perfil publicador dueno de la actividad al momento de pedir.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_publicador_id", nullable = false)
    private PerfilPublicador perfilPublicador;

    /**
     * Usuario autenticado que pidio el cambio.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Estado del flujo: PENDIENTE, EN_REVISION, APROBADA o RECHAZADA.
     */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    // ==========================================================
    // Campos propuestos (null = sin cambio propuesto)
    // ==========================================================

    @Column(name = "titulo", length = 150)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_referencia", precision = 10, scale = 2)
    private BigDecimal precioReferencia;

    @Column(name = "mostrar_precio")
    private Boolean mostrarPrecio;

    @Column(name = "whatsapp_contacto", length = 30)
    private String whatsappContacto;

    @Column(name = "instagram_contacto", length = 150)
    private String instagramContacto;

    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Column(name = "nivel", length = 50)
    private String nivel;

    @Column(name = "modalidad", length = 50)
    private String modalidad;

    // ==========================================================
    // Campos propuestos nuevos (script 24): deporte, edades,
    // enfoque, ubicacion y horarios. Null = sin cambio propuesto.
    // ==========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deporte_id")
    private Deporte deporte;

    @Column(name = "edad_minima")
    private Integer edadMinima;

    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    @Column(name = "enfoque", length = 50)
    private String enfoque;

    @Column(name = "ubicacion_nombre", length = 150)
    private String ubicacionNombre;

    @Column(name = "ubicacion_direccion", length = 255)
    private String ubicacionDireccion;

    @Column(name = "ubicacion_referencia", length = 255)
    private String ubicacionReferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_barrio_id")
    private Barrio ubicacionBarrio;

    /**
     * true = la solicitud propone REEMPLAZAR el conjunto de horarios
     * por las filas hijas. El flag existe porque "cero hijas" no
     * distingue "no toco horarios" de "borrar todos"; con true el
     * service exige al menos una.
     */
    @Column(name = "cambia_horarios", nullable = false)
    private Boolean cambiaHorarios = Boolean.FALSE;

    /** Horarios PROPUESTOS (reemplazo total si cambiaHorarios=true). */
    @jakarta.persistence.OneToMany(
            mappedBy = "solicitud",
            cascade = jakarta.persistence.CascadeType.ALL,
            orphanRemoval = true
    )
    @jakarta.persistence.OrderBy("diaSemana ASC, horaInicio ASC")
    private java.util.List<SolicitudCambioHorario> horarios = new java.util.ArrayList<>();

    // ==========================================================
    // Resolucion administrativa
    // ==========================================================

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por_usuario_id")
    private Usuario resueltoPor;

    @Column(name = "resuelto_at")
    private OffsetDateTime resueltoAt;

    // ==========================================================
    // Auditoria y baja logica
    // ==========================================================

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public PerfilPublicador getPerfilPublicador() {
        return perfilPublicador;
    }

    public void setPerfilPublicador(PerfilPublicador perfilPublicador) {
        this.perfilPublicador = perfilPublicador;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
    }

    public String getWhatsappContacto() {
        return whatsappContacto;
    }

    public void setWhatsappContacto(String whatsappContacto) {
        this.whatsappContacto = whatsappContacto;
    }

    public String getInstagramContacto() {
        return instagramContacto;
    }

    public void setInstagramContacto(String instagramContacto) {
        this.instagramContacto = instagramContacto;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public void setDeporte(Deporte deporte) {
        this.deporte = deporte;
    }

    public Integer getEdadMinima() {
        return edadMinima;
    }

    public void setEdadMinima(Integer edadMinima) {
        this.edadMinima = edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public void setEdadMaxima(Integer edadMaxima) {
        this.edadMaxima = edadMaxima;
    }

    public String getEnfoque() {
        return enfoque;
    }

    public void setEnfoque(String enfoque) {
        this.enfoque = enfoque;
    }

    public String getUbicacionNombre() {
        return ubicacionNombre;
    }

    public void setUbicacionNombre(String ubicacionNombre) {
        this.ubicacionNombre = ubicacionNombre;
    }

    public String getUbicacionDireccion() {
        return ubicacionDireccion;
    }

    public void setUbicacionDireccion(String ubicacionDireccion) {
        this.ubicacionDireccion = ubicacionDireccion;
    }

    public String getUbicacionReferencia() {
        return ubicacionReferencia;
    }

    public void setUbicacionReferencia(String ubicacionReferencia) {
        this.ubicacionReferencia = ubicacionReferencia;
    }

    public Barrio getUbicacionBarrio() {
        return ubicacionBarrio;
    }

    public void setUbicacionBarrio(Barrio ubicacionBarrio) {
        this.ubicacionBarrio = ubicacionBarrio;
    }

    public Boolean getCambiaHorarios() {
        return cambiaHorarios;
    }

    public void setCambiaHorarios(Boolean cambiaHorarios) {
        this.cambiaHorarios = cambiaHorarios;
    }

    public java.util.List<SolicitudCambioHorario> getHorarios() {
        return horarios;
    }

    public void setHorarios(java.util.List<SolicitudCambioHorario> horarios) {
        this.horarios = horarios;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Usuario getResueltoPor() {
        return resueltoPor;
    }

    public void setResueltoPor(Usuario resueltoPor) {
        this.resueltoPor = resueltoPor;
    }

    public OffsetDateTime getResueltoAt() {
        return resueltoAt;
    }

    public void setResueltoAt(OffsetDateTime resueltoAt) {
        this.resueltoAt = resueltoAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
