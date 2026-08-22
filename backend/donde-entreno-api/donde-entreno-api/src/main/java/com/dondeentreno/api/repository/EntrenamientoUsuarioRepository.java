package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.EntrenamientoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

/**
 * Check-ins "Entrené acá" (script 26).
 */
public interface EntrenamientoUsuarioRepository
        extends JpaRepository<EntrenamientoUsuario, Long> {

    /**
     * ¿El usuario ya registró un check-in de esta actividad desde el
     * instante dado? Con `desde` = inicio del día en zona argentina,
     * resuelve la regla "1 por día" contra la base.
     */
    boolean existsByUsuarioIdAndActividadIdAndCreatedAtGreaterThanEqual(
            Long usuarioId,
            Long actividadId,
            OffsetDateTime desde
    );

    /**
     * Personas DISTINTAS que entrenaron en la actividad desde el
     * instante dado. Es el contador público: agregado y anónimo, y
     * distinct para que insistir con el botón no infle el número.
     */
    @Query("""
            SELECT COUNT(DISTINCT e.usuarioId)
              FROM EntrenamientoUsuario e
             WHERE e.actividadId = :actividadId
               AND e.createdAt >= :desde
            """)
    long contarPersonasDesde(
            @Param("actividadId") Long actividadId,
            @Param("desde") OffsetDateTime desde
    );
}
