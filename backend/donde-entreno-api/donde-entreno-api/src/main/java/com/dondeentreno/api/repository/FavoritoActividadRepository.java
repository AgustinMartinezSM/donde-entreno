package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.FavoritoActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Actividades guardadas por usuario (script 20, sync de favoritos).
 */
public interface FavoritoActividadRepository
        extends JpaRepository<FavoritoActividad, Long> {

    boolean existsByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);

    /**
     * Favoritos del usuario, mas recientes primero. Devuelve las filas
     * del favorito (no las actividades): el listado publico se resuelve
     * en un segundo query filtrado, como el feed.
     */
    List<FavoritoActividad> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /**
     * Deja de guardar: borra la fila si existe (idempotente). Devuelve
     * la cantidad borrada (0 si no estaba guardada).
     */
    long deleteByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);
}
