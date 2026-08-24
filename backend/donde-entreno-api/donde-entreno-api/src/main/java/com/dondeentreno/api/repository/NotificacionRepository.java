package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Notificaciones internas (script 28, Fase 2 social).
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    /** El número de la campanita. */
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    /** Marcar TODAS como leídas en un solo UPDATE (sin traerlas). */
    @Modifying
    @Query("""
            UPDATE Notificacion n
               SET n.leida = true
             WHERE n.usuarioId = :usuarioId
               AND n.leida = false
            """)
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);
}
