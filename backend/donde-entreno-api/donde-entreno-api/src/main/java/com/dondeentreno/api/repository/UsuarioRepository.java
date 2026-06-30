package com.dondeentreno.api.repository;

import com.dondeentreno.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository de Usuario.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
            SELECT u
            FROM Usuario u
            JOIN FETCH u.rol
            WHERE LOWER(TRIM(u.email)) = :emailNormalizado
            """)
    Optional<Usuario> findByEmailNormalizado(@Param("emailNormalizado") String emailNormalizado);

    boolean existsByRol_NombreAndActivoTrueAndDeletedAtIsNull(String nombreRol);
}
