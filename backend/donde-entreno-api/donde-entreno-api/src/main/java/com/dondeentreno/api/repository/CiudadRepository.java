package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository de Ciudad.
 *
 * Esta interfaz se encarga de consultar la tabla ciudad
 * usando Spring Data JPA.
 */
public interface CiudadRepository extends JpaRepository<Ciudad, Long> {

    /**
     * Busca todas las ciudades activas ordenadas por nombre.
     *
     * Spring interpreta:
     * WHERE activa = true
     * ORDER BY nombre ASC
     */
    List<Ciudad> findByActivaTrueOrderByNombreAsc();
}