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
import java.net.InetAddress;
import java.time.OffsetDateTime;

/**
 * Entidad que representa la tabla solicitud_publicacion de PostgreSQL.
 *
 * Guarda solicitudes publicas de publicacion enviadas desde el formulario web.
 *
 * La solicitud es independiente de la tabla actividad. Una actividad puede
 * asociarse luego durante el flujo administrativo.
 */
@Entity
@Table(name = "solicitud_publicacion")
public class SolicitudPublicacion {

    /**
     * Identificador unico de la solicitud.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Codigo publico de seguimiento de la solicitud.
     */
    @Column(name = "codigo_seguimiento", nullable = false, unique = true, length = 40)
    private String codigoSeguimiento;

    /**
     * Origen de la solicitud.
     */
    @Column(name = "origen", nullable = false, length = 30)
    private String origen;

    /**
     * Estado del flujo de revision.
     */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /**
     * Tipo de publicador solicitante.
     */
    @Column(name = "tipo_publicador", nullable = false, length = 50)
    private String tipoPublicador;

    /**
     * Nombre del publicador solicitante.
     */
    @Column(name = "nombre_publicador", nullable = false, length = 150)
    private String nombrePublicador;

    /**
     * Nombre de la actividad solicitada.
     */
    @Column(name = "nombre_actividad", nullable = false, length = 150)
    private String nombreActividad;

    /**
     * Deporte existente relacionado con la solicitud, si corresponde.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deporte_id")
    private Deporte deporte;

    /**
     * Deporte escrito por el solicitante, si no eligio uno existente.
     */
    @Column(name = "deporte_otro", length = 100)
    private String deporteOtro;

    /**
     * Descripcion de la actividad solicitada.
     */
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Nivel de la actividad solicitada.
     */
    @Column(name = "nivel", nullable = false, length = 50)
    private String nivel;

    /**
     * Enfoque de la actividad solicitada.
     */
    @Column(name = "enfoque", nullable = false, length = 50)
    private String enfoque;

    /**
     * Modalidad de la actividad solicitada.
     */
    @Column(name = "modalidad", nullable = false, length = 50)
    private String modalidad;

    /**
     * Edad minima informada.
     */
    @Column(name = "edad_minima")
    private Integer edadMinima;

    /**
     * Edad maxima informada.
     */
    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    /**
     * Precio de referencia informado.
     */
    @Column(name = "precio_referencia", precision = 10, scale = 2)
    private BigDecimal precioReferencia;

    /**
     * Indica si el precio debe mostrarse publicamente.
     */
    @Column(name = "mostrar_precio", nullable = false)
    private Boolean mostrarPrecio;

    /**
     * Ciudad existente relacionada con la solicitud, si corresponde.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id")
    private Ciudad ciudad;

    /**
     * Ciudad escrita por el solicitante, si no eligio una existente.
     */
    @Column(name = "ciudad_otra", length = 100)
    private String ciudadOtra;

    /**
     * Barrio existente relacionado con la solicitud, si corresponde.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id")
    private Barrio barrio;

    /**
     * Barrio escrito por el solicitante, si no eligio uno existente.
     */
    @Column(name = "barrio_otro", length = 100)
    private String barrioOtro;

    /**
     * Nombre del lugar propuesto.
     */
    @Column(name = "nombre_lugar", length = 150)
    private String nombreLugar;

    /**
     * Direccion propuesta.
     */
    @Column(name = "direccion", length = 255)
    private String direccion;

    /**
     * Referencia adicional de ubicacion.
     */
    @Column(name = "referencia_ubicacion", length = 255)
    private String referenciaUbicacion;

    /**
     * WhatsApp informado por el solicitante.
     */
    @Column(name = "whatsapp", length = 40)
    private String whatsapp;

    /**
     * WhatsApp normalizado para busquedas y control interno.
     */
    @Column(name = "whatsapp_normalizado", length = 30)
    private String whatsappNormalizado;

    /**
     * Instagram informado por el solicitante.
     */
    @Column(name = "instagram", length = 150)
    private String instagram;

    /**
     * Email informado por el solicitante.
     */
    @Column(name = "email", length = 150)
    private String email;

    /**
     * Observaciones escritas por el solicitante.
     */
    @Column(name = "observaciones_solicitante", columnDefinition = "TEXT")
    private String observacionesSolicitante;

    /**
     * Aceptacion de condiciones del formulario.
     */
    @Column(name = "acepta_condiciones", nullable = false)
    private Boolean aceptaCondiciones;

    /**
     * Usuario que envio la solicitud cuando el flujo es autenticado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * Perfil publicador que envio la solicitud cuando el flujo es autenticado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_publicador_id")
    private PerfilPublicador perfilPublicador;

    /**
     * Usuario que revisa o reviso la solicitud.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_usuario_id")
    private Usuario revisadoPorUsuario;

    /**
     * Actividad generada a partir de la solicitud, si fue aprobada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_generada_id", unique = true)
    private Actividad actividadGenerada;

    /**
     * Motivo de rechazo administrativo.
     */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    /**
     * Observaciones internas de revision.
     */
    @Column(name = "observaciones_revision", columnDefinition = "TEXT")
    private String observacionesRevision;

    /**
     * Fecha de inicio de revision.
     */
    @Column(name = "revision_iniciada_at")
    private OffsetDateTime revisionIniciadaAt;

    /**
     * Fecha de finalizacion de revision.
     */
    @Column(name = "revision_finalizada_at")
    private OffsetDateTime revisionFinalizadaAt;

    /**
     * IP de origen de la solicitud.
     */
    @Column(name = "ip_origen", columnDefinition = "inet")
    private InetAddress ipOrigen;

    /**
     * Fecha de creacion.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Fecha de actualizacion.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Fecha de borrado logico.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Constructor vacio obligatorio para JPA.
     */
    public SolicitudPublicacion() {
    }

    public Long getId() {
        return id;
    }

    public String getCodigoSeguimiento() {
        return codigoSeguimiento;
    }

    public String getOrigen() {
        return origen;
    }

    public String getEstado() {
        return estado;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public String getNombrePublicador() {
        return nombrePublicador;
    }

    public String getNombreActividad() {
        return nombreActividad;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public String getDeporteOtro() {
        return deporteOtro;
    }

    public String getDescripcion() {
        return descripcion;
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

    public Integer getEdadMinima() {
        return edadMinima;
    }

    public Integer getEdadMaxima() {
        return edadMaxima;
    }

    public BigDecimal getPrecioReferencia() {
        return precioReferencia;
    }

    public Boolean getMostrarPrecio() {
        return mostrarPrecio;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }

    public String getCiudadOtra() {
        return ciudadOtra;
    }

    public Barrio getBarrio() {
        return barrio;
    }

    public String getBarrioOtro() {
        return barrioOtro;
    }

    public String getNombreLugar() {
        return nombreLugar;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferenciaUbicacion() {
        return referenciaUbicacion;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getWhatsappNormalizado() {
        return whatsappNormalizado;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getEmail() {
        return email;
    }

    public String getObservacionesSolicitante() {
        return observacionesSolicitante;
    }

    public Boolean getAceptaCondiciones() {
        return aceptaCondiciones;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public PerfilPublicador getPerfilPublicador() {
        return perfilPublicador;
    }

    public Usuario getRevisadoPorUsuario() {
        return revisadoPorUsuario;
    }

    public Actividad getActividadGenerada() {
        return actividadGenerada;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public String getObservacionesRevision() {
        return observacionesRevision;
    }

    public OffsetDateTime getRevisionIniciadaAt() {
        return revisionIniciadaAt;
    }

    public OffsetDateTime getRevisionFinalizadaAt() {
        return revisionFinalizadaAt;
    }

    public InetAddress getIpOrigen() {
        return ipOrigen;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigoSeguimiento(String codigoSeguimiento) {
        this.codigoSeguimiento = codigoSeguimiento;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public void setNombrePublicador(String nombrePublicador) {
        this.nombrePublicador = nombrePublicador;
    }

    public void setNombreActividad(String nombreActividad) {
        this.nombreActividad = nombreActividad;
    }

    public void setDeporte(Deporte deporte) {
        this.deporte = deporte;
    }

    public void setDeporteOtro(String deporteOtro) {
        this.deporteOtro = deporteOtro;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public void setEdadMinima(Integer edadMinima) {
        this.edadMinima = edadMinima;
    }

    public void setEdadMaxima(Integer edadMaxima) {
        this.edadMaxima = edadMaxima;
    }

    public void setPrecioReferencia(BigDecimal precioReferencia) {
        this.precioReferencia = precioReferencia;
    }

    public void setMostrarPrecio(Boolean mostrarPrecio) {
        this.mostrarPrecio = mostrarPrecio;
    }

    public void setCiudad(Ciudad ciudad) {
        this.ciudad = ciudad;
    }

    public void setCiudadOtra(String ciudadOtra) {
        this.ciudadOtra = ciudadOtra;
    }

    public void setBarrio(Barrio barrio) {
        this.barrio = barrio;
    }

    public void setBarrioOtro(String barrioOtro) {
        this.barrioOtro = barrioOtro;
    }

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setReferenciaUbicacion(String referenciaUbicacion) {
        this.referenciaUbicacion = referenciaUbicacion;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void setWhatsappNormalizado(String whatsappNormalizado) {
        this.whatsappNormalizado = whatsappNormalizado;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setObservacionesSolicitante(String observacionesSolicitante) {
        this.observacionesSolicitante = observacionesSolicitante;
    }

    public void setAceptaCondiciones(Boolean aceptaCondiciones) {
        this.aceptaCondiciones = aceptaCondiciones;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setPerfilPublicador(PerfilPublicador perfilPublicador) {
        this.perfilPublicador = perfilPublicador;
    }

    public void setRevisadoPorUsuario(Usuario revisadoPorUsuario) {
        this.revisadoPorUsuario = revisadoPorUsuario;
    }

    public void setActividadGenerada(Actividad actividadGenerada) {
        this.actividadGenerada = actividadGenerada;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public void setObservacionesRevision(String observacionesRevision) {
        this.observacionesRevision = observacionesRevision;
    }

    public void setRevisionIniciadaAt(OffsetDateTime revisionIniciadaAt) {
        this.revisionIniciadaAt = revisionIniciadaAt;
    }

    public void setRevisionFinalizadaAt(OffsetDateTime revisionFinalizadaAt) {
        this.revisionFinalizadaAt = revisionFinalizadaAt;
    }

    public void setIpOrigen(InetAddress ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
