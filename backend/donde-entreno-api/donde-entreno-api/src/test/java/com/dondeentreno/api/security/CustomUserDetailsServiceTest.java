package com.dondeentreno.api.security;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void usuarioActivoVerificadoYNoEliminadoDevuelveUserDetails() {
        Usuario usuario = crearUsuario(
                "admin@dondeentreno.com",
                "$2a$10$hash-de-prueba-no-real",
                true,
                true,
                null,
                "ADMIN"
        );

        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@dondeentreno.com");

        UsuarioPrincipal usuarioPrincipal = assertInstanceOf(UsuarioPrincipal.class, userDetails);
        assertEquals(1L, usuarioPrincipal.getId());
        assertEquals("Admin", usuarioPrincipal.getNombre());
        assertEquals("SUPER", usuarioPrincipal.getApellido());
        assertEquals("ADMIN", usuarioPrincipal.getRol());
        assertEquals("admin@dondeentreno.com", userDetails.getUsername());
        assertEquals("$2a$10$hash-de-prueba-no-real", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void emailConEspaciosYMayusculasSeBuscaNormalizado() {
        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.of(crearUsuario(
                        "admin@dondeentreno.com",
                        "$2a$10$hash-de-prueba-no-real",
                        true,
                        true,
                        null,
                        "ADMIN"
                )));

        customUserDetailsService.loadUserByUsername("  ADMIN@DONDEENTRENO.COM  ");

        verify(usuarioRepository).findByEmailNormalizado("admin@dondeentreno.com");
    }

    @Test
    void usuarioNoEncontradoLanzaUsernameNotFoundException() {
        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("admin@dondeentreno.com")
        );
    }

    @Test
    void usuarioInactivoQuedaDisabled() {
        Usuario usuario = crearUsuario(
                "admin@dondeentreno.com",
                "$2a$10$hash-de-prueba-no-real",
                false,
                true,
                null,
                "ADMIN"
        );

        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@dondeentreno.com");

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void usuarioNoVerificadoQuedaDisabled() {
        Usuario usuario = crearUsuario(
                "admin@dondeentreno.com",
                "$2a$10$hash-de-prueba-no-real",
                true,
                false,
                null,
                "ADMIN"
        );

        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@dondeentreno.com");

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void usuarioEliminadoQuedaDisabled() {
        Usuario usuario = crearUsuario(
                "admin@dondeentreno.com",
                "$2a$10$hash-de-prueba-no-real",
                true,
                true,
                OffsetDateTime.now(),
                "ADMIN"
        );

        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@dondeentreno.com");

        assertFalse(userDetails.isEnabled());
    }

    private Usuario crearUsuario(
            String email,
            String passwordHash,
            boolean activo,
            boolean emailVerificado,
            OffsetDateTime deletedAt,
            String rolNombre
    ) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Admin");
        usuario.setApellido("SUPER");
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordHash);
        usuario.setActivo(activo);
        usuario.setEmailVerificado(emailVerificado);
        usuario.setDeletedAt(deletedAt);
        usuario.setRol(rol);

        return usuario;
    }
}
