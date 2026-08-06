package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.SolicitudCambioActividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a solicitudes de cambio de actividades publicadas.
 */
public interface SolicitudCambioActividadRepository
        extends JpaRepository<SolicitudCambioActividad, Long> {

    /**
     * Detecta si la actividad ya tiene una solicitud abierta
     * (PENDIENTE o EN_REVISION): solo puede haber una a la vez.
     */
    boolean existsByActividad_IdAndEstadoInAndDeletedAtIsNull(
            Long actividadId,
            List<String> estados
    );

    /**
     * Listado del publicador (todas las suyas, con paginado).
     */
    Page<SolicitudCambioActividad> findByPerfilPublicador_IdAndDeletedAtIsNull(
            Long perfilPublicadorId,
            Pageable pageable
    );

    /**
     * Cuenta las solicitudes de cambio propias en alguno de los estados
     * dados (métricas del panel: se usa con los estados abiertos
     * PENDIENTE y EN_REVISION).
     */
    long countByPerfilPublicador_IdAndEstadoInAndDeletedAtIsNull(
            Long perfilPublicadorId,
            List<String> estados
    );

    /**
     * Listado del publicador filtrado por estado.
     */
    Page<SolicitudCambioActividad> findByPerfilPublicador_IdAndEstadoAndDeletedAtIsNull(
            Long perfilPublicadorId,
            String estado,
            Pageable pageable
    );

    /**
     * Detalle propio: valida ownership por perfil.
     */
    Optional<SolicitudCambioActividad> findByIdAndPerfilPublicador_IdAndDeletedAtIsNull(
            Long id,
            Long perfilPublicadorId
    );

    /**
     * Cola de revision del admin (todas).
     */
    Page<SolicitudCambioActividad> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Cola de revision del admin filtrada por estado.
     */
    Page<SolicitudCambioActividad> findByEstadoAndDeletedAtIsNull(
            String estado,
            Pageable pageable
    );

    /**
     * Detalle para el admin.
     */
    Optional<SolicitudCambioActividad> findByIdAndDeletedAtIsNull(Long id);
}
