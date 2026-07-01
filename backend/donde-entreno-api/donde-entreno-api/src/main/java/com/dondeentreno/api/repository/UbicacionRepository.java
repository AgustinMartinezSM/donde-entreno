package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de Ubicacion.
 *
 * Esta interfaz se encarga de consultar la tabla ubicacion
 * usando Spring Data JPA.
 */
public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    /**
     * Busca todas las ubicaciones activas ordenadas por nombre.
     *
     * Spring interpreta:
     * WHERE activa = true
     * ORDER BY nombre ASC
     */
    List<Ubicacion> findByActivaTrueOrderByNombreAsc();

    /**
     * Busca ubicaciones activas filtradas por perfil publicador.
     *
     * Ejemplo:
     * ubicaciones de un club, gimnasio o profesor.
     */
    List<Ubicacion> findByActivaTrueAndPerfilPublicador_IdOrderByNombreAsc(Long perfilPublicadorId);

    /**
     * Busca ubicaciones activas filtradas por ciudad.
     */
    List<Ubicacion> findByActivaTrueAndCiudad_IdOrderByNombreAsc(Long ciudadId);

    /**
     * Busca ubicaciones activas filtradas por barrio.
     */
    List<Ubicacion> findByActivaTrueAndBarrio_IdOrderByNombreAsc(Long barrioId);

    Optional<Ubicacion> findFirstByPerfilPublicador_IdAndCiudad_IdAndBarrio_IdAndNombreIgnoreCaseAndDireccionIgnoreCaseAndActivaTrueAndDeletedAtIsNull(
            Long perfilPublicadorId,
            Long ciudadId,
            Long barrioId,
            String nombre,
            String direccion
    );
}
