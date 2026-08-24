package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reportes de contenido (script 28, Fase 2 social).
 */
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    boolean existsByUsuarioIdAndTipoObjetoAndObjetoId(
            Long usuarioId,
            String tipoObjeto,
            Long objetoId
    );

    Page<Reporte> findByEstadoOrderByCreatedAtDesc(String estado, Pageable pageable);

    Page<Reporte> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Para el panel: cuántos pendientes hay. */
    long countByEstado(String estado);
}
