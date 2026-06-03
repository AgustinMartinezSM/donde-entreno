package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Deporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de Deporte.
 *
 * Esta interfaz se encarga de consultar la tabla deporte
 * usando Spring Data JPA.
 *
 * No hace falta implementar los métodos manualmente.
 * Spring Data JPA genera las consultas automáticamente
 * a partir del nombre de cada método.
 */
public interface DeporteRepository extends JpaRepository<Deporte, Long> {

    /**
     * Busca todos los deportes activos.
     *
     * Spring interpreta:
     * WHERE activo = true
     */
    List<Deporte> findByActivoTrue();

    /**
     * Busca todos los deportes activos ordenados por el campo orden.
     *
     * Esto nos sirve para mostrar los deportes prolijos en pantalla.
     */
    List<Deporte> findByActivoTrueOrderByOrdenAsc();

    /**
     * Busca un deporte por su slug.
     *
     * Ejemplo:
     * "boxeo"
     */
    Optional<Deporte> findBySlug(String slug);

    /**
     * Busca deportes activos por ID de categoría deportiva.
     *
     * Como Deporte tiene una relación con CategoriaDeportiva,
     * usamos:
     *
     * categoriaDeportiva_Id
     *
     * Eso significa:
     * "entrar a categoriaDeportiva y filtrar por su id".
     */
    List<Deporte> findByActivoTrueAndCategoriaDeportiva_IdOrderByOrdenAsc(Long categoriaDeportivaId);

    /**
     * Busca deportes activos por slug de categoría deportiva.
     *
     * Esto nos va a servir para filtrar deportes por categoría usando URLs amigables.
     *
     * Ejemplo:
     * categoriaSlug = "deportes-de-combate"
     */
    List<Deporte> findByActivoTrueAndCategoriaDeportiva_SlugOrderByOrdenAsc(String categoriaSlug);
}