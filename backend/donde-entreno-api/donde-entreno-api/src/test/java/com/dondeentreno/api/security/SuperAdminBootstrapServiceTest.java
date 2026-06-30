package com.dondeentreno.api.security;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapServiceTest {

    private static final String PASSWORD_FICTICIA = "PasswordFicticia123!";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void bootstrapDeshabilitadoNoBuscaRolNiGuardaUsuario() {
        SuperAdminBootstrapProperties properties = new SuperAdminBootstrapProperties();
        properties.setEnabled("false");
        SuperAdminBootstrapService service = crearService(properties);

        service.ejecutarBootstrapSiCorresponde();

        verifyNoInteractions(usuarioRepository, rolRepository);
    }

    @Test
    void bootstrapHabilitadoPeroYaExisteSuperAdminNoExigeVariablesNiGuardaUsuario() {
        SuperAdminBootstrapProperties properties = new SuperAdminBootstrapProperties();
        properties.setEnabled("true");
        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(true);
        SuperAdminBootstrapService service = crearService(properties);

        service.ejecutarBootstrapSiCorresponde();

        verify(usuarioRepository).existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN");
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(rolRepository);
    }

    @Test
    void bootstrapHabilitadoSinSuperAdminCreaUsuario() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();
        Rol rol = rolSuperAdmin(true);

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        when(rolRepository.findByNombre("SUPER_ADMIN")).thenReturn(Optional.of(rol));
        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com")).thenReturn(Optional.empty());
        SuperAdminBootstrapService service = crearService(properties);

        service.ejecutarBootstrapSiCorresponde();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());

        Usuario usuario = usuarioCaptor.getValue();
        assertEquals("admin@dondeentreno.com", usuario.getEmail());
        assertEquals("Admin Inicial", usuario.getNombre());
        assertEquals("Bootstrap", usuario.getApellido());
        assertEquals(rol, usuario.getRol());
        assertNotEquals(PASSWORD_FICTICIA, usuario.getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD_FICTICIA, usuario.getPasswordHash()));
        assertTrue(usuario.getActivo());
        assertTrue(usuario.getEmailVerificado());
        assertNull(usuario.getTelefono());
        assertNull(usuario.getUltimoLoginAt());
        assertNull(usuario.getDeletedAt());
        assertNotNull(usuario.getCreatedAt());
        assertNotNull(usuario.getUpdatedAt());
        assertEquals(usuario.getCreatedAt(), usuario.getUpdatedAt());
    }

    @Test
    void faltaEmailLanzaExcepcionYNoGuarda() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();
        properties.setEmail(" ");

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        SuperAdminBootstrapService service = crearService(properties);

        assertThrows(IllegalStateException.class, service::ejecutarBootstrapSiCorresponde);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(rolRepository);
    }

    @Test
    void faltaPasswordLanzaExcepcionYNoGuarda() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();
        properties.setPassword(null);

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        SuperAdminBootstrapService service = crearService(properties);

        assertThrows(IllegalStateException.class, service::ejecutarBootstrapSiCorresponde);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(rolRepository);
    }

    @Test
    void passwordDemasiadoCortaLanzaExcepcionYNoGuarda() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();
        properties.setPassword("corta");

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        SuperAdminBootstrapService service = crearService(properties);

        assertThrows(IllegalStateException.class, service::ejecutarBootstrapSiCorresponde);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(rolRepository);
    }

    @Test
    void rolSuperAdminNoExisteLanzaExcepcionYNoGuarda() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        when(rolRepository.findByNombre("SUPER_ADMIN")).thenReturn(Optional.empty());
        SuperAdminBootstrapService service = crearService(properties);

        assertThrows(IllegalStateException.class, service::ejecutarBootstrapSiCorresponde);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rolSuperAdminInactivoLanzaExcepcionYNoGuarda() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        when(rolRepository.findByNombre("SUPER_ADMIN")).thenReturn(Optional.of(rolSuperAdmin(false)));
        SuperAdminBootstrapService service = crearService(properties);

        assertThrows(IllegalStateException.class, service::ejecutarBootstrapSiCorresponde);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioConEmailNormalizadoExistenteLanzaExcepcionYNoGuarda() {
        SuperAdminBootstrapProperties properties = propiedadesCompletas();

        when(usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull("SUPER_ADMIN"))
                .thenReturn(false);
        when(rolRepository.findByNombre("SUPER_ADMIN")).thenReturn(Optional.of(rolSuperAdmin(true)));
        when(usuarioRepository.findByEmailNormalizado("admin@dondeentreno.com"))
                .thenReturn(Optional.of(new Usuario()));
        SuperAdminBootstrapService service = crearService(properties);

        assertThrows(IllegalStateException.class, service::ejecutarBootstrapSiCorresponde);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private SuperAdminBootstrapService crearService(SuperAdminBootstrapProperties properties) {
        return new SuperAdminBootstrapService(
                properties,
                usuarioRepository,
                rolRepository,
                passwordEncoder
        );
    }

    private SuperAdminBootstrapProperties propiedadesCompletas() {
        SuperAdminBootstrapProperties properties = new SuperAdminBootstrapProperties();
        properties.setEnabled("true");
        properties.setEmail("  ADMIN@DONDEENTRENO.COM  ");
        properties.setPassword(PASSWORD_FICTICIA);
        properties.setNombre(" Admin Inicial ");
        properties.setApellido(" Bootstrap ");
        return properties;
    }

    private Rol rolSuperAdmin(boolean activo) {
        Rol rol = new Rol();
        rol.setNombre("SUPER_ADMIN");
        rol.setActivo(activo);
        return rol;
    }
}
