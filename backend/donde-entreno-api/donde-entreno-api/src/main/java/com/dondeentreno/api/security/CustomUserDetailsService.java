package com.dondeentreno.api.security;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Carga usuarios de DondeEntreno para Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Credenciales invalidas.";

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String emailNormalizado = normalizarEmail(username);
        Usuario usuario = usuarioRepository.findByEmailNormalizado(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException(MENSAJE_CREDENCIALES_INVALIDAS));

        Rol rol = usuario.getRol();
        if (rol == null || rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new UsernameNotFoundException(MENSAJE_CREDENCIALES_INVALIDAS);
        }

        return UsuarioPrincipal.desdeUsuario(usuario);
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
