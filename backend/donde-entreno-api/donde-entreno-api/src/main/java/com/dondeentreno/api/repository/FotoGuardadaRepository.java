package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.FotoGuardada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Fotos guardadas (script 30, patrón MeGustaImagenRepository).
 */
public interface FotoGuardadaRepository extends JpaRepository<FotoGuardada, Long> {

    boolean existsByUsuarioIdAndImagenId(Long usuarioId, Long imagenId);

    long deleteByUsuarioIdAndImagenId(Long usuarioId, Long imagenId);

    List<FotoGuardada> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /** Ids guardados del usuario, para pintar los bookmarks. */
    @Query("SELECT f.imagenId FROM FotoGuardada f WHERE f.usuarioId = :usuarioId")
    List<Long> imagenIdsDe(@Param("usuarioId") Long usuarioId);
}
