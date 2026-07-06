package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.PerfilPublicador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de PerfilPublicador.
 *
 * Esta interfaz se encarga de consultar la tabla perfil_publicador
 * usando Spring Data JPA.
 */
public interface PerfilPublicadorRepository extends JpaRepository<PerfilPublicador, Long> {

    /**
     * Busca todos los perfiles publicadores activos,
     * ordenados alfabéticamente por nombre.
     *
     * Spring interpreta:
     * WHERE activo = true
     * ORDER BY nombre ASC
     */
    List<PerfilPublicador> findByActivoTrueOrderByNombreAsc();

    /**
     * Busca perfiles activos filtrando por tipo de publicador.
     *
     * Ejemplos de tipo:
     * CLUB
     * GIMNASIO
     * PROFESOR_INDEPENDIENTE
     * INSTITUCION
     * ESCUELA_DEPORTIVA
     * ESPACIO_ENTRENAMIENTO
     */
    List<PerfilPublicador> findByActivoTrueAndTipoPublicadorOrderByNombreAsc(String tipoPublicador);

    /**
     * Busca un perfil activo por ID.
     *
     * Lo vamos a usar más adelante para ver detalle de un perfil.
     */
    Optional<PerfilPublicador> findByIdAndActivoTrue(Long id);

    Optional<PerfilPublicador> findFirstByUsuario_IdAndTipoPublicadorIgnoreCaseAndNombreIgnoreCaseAndActivoTrueAndDeletedAtIsNull(
            Long usuarioId,
            String tipoPublicador,
            String nombre
    );

    Optional<PerfilPublicador> findFirstByUsuario_IdAndActivoTrueAndDeletedAtIsNull(Long usuarioId);

    boolean existsByUsuario_IdAndActivoTrueAndDeletedAtIsNull(Long usuarioId);
}
