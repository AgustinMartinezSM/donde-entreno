package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Barrio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository de Barrio.
 *
 * Esta interfaz se encarga de consultar la tabla barrio
 * usando Spring Data JPA.
 */
public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    /**
     * Busca todos los barrios activos ordenados por nombre.
     *
     * Spring interpreta:
     * WHERE activo = true
     * ORDER BY nombre ASC
     */
    List<Barrio> findByActivoTrueOrderByNombreAsc();

    /**
     * Busca barrios activos filtrando por ID de ciudad.
     *
     * Como Barrio tiene una relación con Ciudad, usamos:
     *
     * ciudad_Id
     *
     * Eso significa:
     * "entrar a la relación ciudad y filtrar por su id".
     */
    List<Barrio> findByActivoTrueAndCiudad_IdOrderByNombreAsc(Long ciudadId);
}