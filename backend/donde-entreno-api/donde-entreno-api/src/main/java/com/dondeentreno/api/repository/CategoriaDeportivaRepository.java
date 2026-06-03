package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.CategoriaDeportiva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de CategoriaDeportiva.
 *
 * Esta interfaz se encarga de comunicarse con la tabla categoria_deportiva
 * usando Spring Data JPA.
 *
 * No hace falta implementar esta interfaz manualmente.
 * Spring crea la implementación automáticamente en tiempo de ejecución.
 */
public interface CategoriaDeportivaRepository extends JpaRepository<CategoriaDeportiva, Long> {

    /**
     * Busca todas las categorías deportivas activas.
     *
     * Spring Data JPA interpreta el nombre del método y arma la consulta:
     * WHERE activa = true
     */
    List<CategoriaDeportiva> findByActivaTrue();

    /**
     * Busca una categoría deportiva por su slug.
     *
     * Ejemplo de slug:
     * "deportes-de-combate"
     */
    Optional<CategoriaDeportiva> findBySlug(String slug);

    /**
     * Busca categorías activas ordenadas por el campo orden de menor a mayor.
     *
     * Esto nos va a servir para mostrar las categorías ordenadas
     * en el frontend.
     */
    List<CategoriaDeportiva> findByActivaTrueOrderByOrdenAsc();
}