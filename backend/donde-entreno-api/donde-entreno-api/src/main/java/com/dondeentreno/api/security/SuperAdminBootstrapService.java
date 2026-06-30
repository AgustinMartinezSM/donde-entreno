package com.dondeentreno.api.security;

import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Crea el primer SUPER_ADMIN solo cuando el bootstrap esta habilitado
 * explicitamente por variables de entorno.
 */
@Service
public class SuperAdminBootstrapService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperAdminBootstrapService.class);
    private static final String ROL_SUPER_ADMIN = "SUPER_ADMIN";
    private static final int PASSWORD_MINIMA = 12;

    private final SuperAdminBootstrapProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminBootstrapService(
            SuperAdminBootstrapProperties properties,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ejecutarBootstrapSiCorresponde();
    }

    void ejecutarBootstrapSiCorresponde() {
        if (!"true".equals(properties.getEnabled())) {
            return;
        }

        if (usuarioRepository.existsByRol_NombreAndActivoTrueAndDeletedAtIsNull(ROL_SUPER_ADMIN)) {
            LOGGER.info("Bootstrap SUPER_ADMIN habilitado, pero ya existe un SUPER_ADMIN activo.");
            return;
        }

        String emailNormalizado = normalizarEmail(requerirTexto(properties.getEmail(), "email"));
        String passwordPlano = requerirTexto(properties.getPassword(), "password");
        String nombre = requerirTexto(properties.getNombre(), "nombre").trim();
        String apellido = textoOpcional(properties.getApellido());

        if (passwordPlano.length() < PASSWORD_MINIMA) {
            throw new IllegalStateException("La password de bootstrap SUPER_ADMIN debe tener al menos 12 caracteres.");
        }

        Rol rol = rolRepository.findByNombre(ROL_SUPER_ADMIN)
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .orElseThrow(() -> new IllegalStateException("El rol SUPER_ADMIN no existe o no esta activo."));

        if (usuarioRepository.findByEmailNormalizado(emailNormalizado).isPresent()) {
            throw new IllegalStateException("Ya existe un usuario con el email de bootstrap normalizado.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setTelefono(null);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setUltimoLoginAt(null);
        usuario.setDeletedAt(null);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);

        usuarioRepository.save(usuario);

        LOGGER.info("Bootstrap SUPER_ADMIN completo: usuario inicial creado de forma segura.");
    }

    private String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Bootstrap SUPER_ADMIN habilitado pero falta configurar " + campo + ".");
        }

        return valor;
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
