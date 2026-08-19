package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokens por sesion iniciada (script 19).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoca la familia entera (logout, o reuso detectado). Idempotente:
     * los ya revocados conservan su primer revocado_en.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken r
               SET r.revocadoEn = :ahora
             WHERE r.familia = :familia
               AND r.revocadoEn IS NULL
            """)
    int revocarFamilia(@Param("familia") UUID familia, @Param("ahora") OffsetDateTime ahora);

    /**
     * Higiene sin scheduler: borra los tokens del usuario vencidos hace
     * mas del limite. Corre en cada login, asi la tabla no crece sin
     * tope.
     */
    @Modifying
    @Query("""
            DELETE FROM RefreshToken r
             WHERE r.usuarioId = :usuarioId
               AND r.expiraEn < :limite
            """)
    int borrarVencidosDe(@Param("usuarioId") Long usuarioId, @Param("limite") OffsetDateTime limite);
}
