package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de Ciudad.
 *
 * Esta interfaz se encarga de consultar la tabla ciudad
 * usando Spring Data JPA.
 */
public interface CiudadRepository extends JpaRepository<Ciudad, Long> {

    /**
     * Busca todas las ciudades activas ordenadas por orden editorial y nombre.
     *
     * Spring interpreta:
     * WHERE activa = true
     * ORDER BY orden ASC, nombre ASC
     */
    List<Ciudad> findByActivaTrueOrderByOrdenAscNombreAsc();

    List<Ciudad> findByActivaTrueOrderByNombreAsc();

    Optional<Ciudad> findBySlugAndActivaTrue(String slug);

    Optional<Ciudad> findByIdAndActivaTrue(Long id);
}
