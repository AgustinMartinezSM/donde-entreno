package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.ColeccionGuardados;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Colecciones de guardados por usuario (script 22, bloque 13).
 */
public interface ColeccionGuardadosRepository
        extends JpaRepository<ColeccionGuardados, Long> {

    List<ColeccionGuardados> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    /** Siempre por dueño: una coleccion ajena no existe para este usuario. */
    Optional<ColeccionGuardados> findByIdAndUsuarioId(Long id, Long usuarioId);
}
