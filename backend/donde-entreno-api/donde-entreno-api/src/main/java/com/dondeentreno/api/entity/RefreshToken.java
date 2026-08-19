package com.dondeentreno.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un refresh token opaco de una sesion iniciada (script 19).
 *
 * Guarda SOLO el SHA-256 del token: el valor en claro viaja una vez al
 * cliente y no se persiste nunca. La familia agrupa la cadena de
 * rotaciones de un mismo login: detectado un reuso, cae completa.
 *
 * El usuario va como FK plana (y no @ManyToOne) a proposito: este
 * registro nunca necesita navegar a la entidad — el servicio solo lee
 * el id para recargar el usuario con su propio query filtrado por
 * activo/deleted.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "familia", nullable = false)
    private UUID familia;

    @Column(name = "emitido_en", nullable = false)
    private OffsetDateTime emitidoEn;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "usado_en")
    private OffsetDateTime usadoEn;

    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;

    public RefreshToken() {
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getFamilia() {
        return familia;
    }

    public void setFamilia(UUID familia) {
        this.familia = familia;
    }

    public OffsetDateTime getEmitidoEn() {
        return emitidoEn;
    }

    public void setEmitidoEn(OffsetDateTime emitidoEn) {
        this.emitidoEn = emitidoEn;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(OffsetDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public OffsetDateTime getUsadoEn() {
        return usadoEn;
    }

    public void setUsadoEn(OffsetDateTime usadoEn) {
        this.usadoEn = usadoEn;
    }

    public OffsetDateTime getRevocadoEn() {
        return revocadoEn;
    }

    public void setRevocadoEn(OffsetDateTime revocadoEn) {
        this.revocadoEn = revocadoEn;
    }
}
