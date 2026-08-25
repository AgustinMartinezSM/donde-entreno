package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.MeGustaAviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Reacciones a avisos del grupo (script 38).
 */
public interface MeGustaAvisoRepository extends JpaRepository<MeGustaAviso, Long> {

    Optional<MeGustaAviso> findByUsuarioIdAndAvisoId(Long usuarioId, Long avisoId);

    boolean existsByUsuarioIdAndAvisoId(Long usuarioId, Long avisoId);

    long countByAvisoId(Long avisoId);

    @Query("""
            SELECT m.avisoId AS avisoId, COUNT(m) AS cantidad
              FROM MeGustaAviso m
             WHERE m.avisoId IN :avisoIds
             GROUP BY m.avisoId
            """)
    List<ConteoMeGustaAviso> contarPorAvisos(@Param("avisoIds") List<Long> avisoIds);

    @Query("""
            SELECT m.avisoId FROM MeGustaAviso m
             WHERE m.usuarioId = :usuarioId
               AND m.avisoId IN :avisoIds
            """)
    List<Long> avisoIdsConMeGustaDe(
            @Param("usuarioId") Long usuarioId,
            @Param("avisoIds") List<Long> avisoIds
    );

    interface ConteoMeGustaAviso {
        Long getAvisoId();

        Long getCantidad();
    }
}
