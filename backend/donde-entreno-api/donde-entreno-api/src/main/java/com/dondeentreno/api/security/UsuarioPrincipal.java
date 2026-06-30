package com.dondeentreno.api.security;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Principal propio para exponer datos seguros del usuario autenticado.
 */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String nombre;
    private final String apellido;
    private final String rol;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public UsuarioPrincipal(
            Long id,
            String email,
            String nombre,
            String apellido,
            String rol,
            String passwordHash,
            boolean enabled
    ) {
        this.id = id;
        this.email = normalizarEmail(email);
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    public static UsuarioPrincipal desdeUsuario(Usuario usuario) {
        Rol rolUsuario = usuario.getRol();
        String nombreRol = rolUsuario == null ? null : rolUsuario.getNombre();
        if (nombreRol == null || nombreRol.isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene un rol valido.");
        }

        boolean habilitado = Boolean.TRUE.equals(usuario.getActivo())
                && Boolean.TRUE.equals(usuario.getEmailVerificado())
                && usuario.getDeletedAt() == null;

        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido(),
                nombreRol,
                usuario.getPasswordHash(),
                habilitado
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private static String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
