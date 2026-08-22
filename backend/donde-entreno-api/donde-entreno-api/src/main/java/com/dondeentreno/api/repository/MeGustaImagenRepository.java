package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.MeGustaImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Likes de usuarios sobre fotos (script 23, bloque 14).
 */
public interface MeGustaImagenRepository extends JpaRepository<MeGustaImagen, Long> {

    boolean existsByUsuarioIdAndImagenId(Long usuarioId, Long imagenId);

    /** Quitar es idempotente: devuelve cuantas filas cayeron (0 o 1). */
    long deleteByUsuarioIdAndImagenId(Long usuarioId, Long imagenId);

    /** Los ids de fotos con like del usuario, para pintar corazones. */
    @Query("SELECT m.imagenId FROM MeGustaImagen m WHERE m.usuarioId = :usuarioId")
    List<Long> imagenIdsDe(@Param("usuarioId") Long usuarioId);

    /**
     * Contador publico por foto en UN query agrupado (patron del
     * contador de seguidores): sin N+1 en las galerias.
     */
    @Query("""
            SELECT m.imagenId, COUNT(m)
              FROM MeGustaImagen m
             WHERE m.imagenId IN :imagenIds
             GROUP BY m.imagenId
            """)
    List<Object[]> contarPorImagen(@Param("imagenIds") Collection<Long> imagenIds);
}
