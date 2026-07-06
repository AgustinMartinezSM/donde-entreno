package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.AuthUsuarioDTO;
import com.dondeentreno.api.dto.LoginRequestDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.dto.RegistroPublicadorRequestDTO;
import com.dondeentreno.api.dto.RegistroUsuarioRequestDTO;
import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.ConfiguracionSistemaInvalidaException;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.EmailYaRegistradoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.RegistroInvalidoException;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.security.JwtService;
import com.dondeentreno.api.security.UsuarioPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio de autenticacion por email/password.
 */
@Service
public class AuthService {

    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_PUBLICADOR = "PUBLICADOR";
    private static final String ESTADO_PERFIL_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Email o password invalidos.";
    private static final int MAX_LONGITUD_TELEFONO_NORMALIZADO = 30;
    private static final Set<String> TIPOS_PUBLICADOR_VALIDOS = Set.of(
            "CLUB",
            "GIMNASIO",
            "PROFESOR_INDEPENDIENTE",
            "INSTITUCION",
            "ESCUELA_DEPORTIVA",
            "ESPACIO_ENTRENAMIENTO"
    );

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final CiudadRepository ciudadRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            CiudadRepository ciudadRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.ciudadRepository = ciudadRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String emailNormalizado = normalizarEmail(request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailNormalizado, request.getPassword())
            );

            UsuarioPrincipal usuario = obtenerUsuarioPrincipal(authentication);
            String accessToken = jwtService.generarAccessToken(usuario);

            return new LoginResponseDTO(
                    "Bearer",
                    accessToken,
                    jwtService.getExpiresIn(),
                    AuthUsuarioDTO.desdePrincipal(usuario)
            );
        } catch (AuthenticationException exception) {
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
        }
    }

    @Transactional
    public LoginResponseDTO registrarUsuario(RegistroUsuarioRequestDTO request) {
        if (request == null) {
            throw new RegistroInvalidoException("El registro no puede estar vacio.");
        }

        String emailNormalizado = normalizarEmail(request.getEmail());
        validarEmailDisponible(emailNormalizado);
        validarPassword(request.getPassword(), request.getConfirmarPassword());

        Rol rol = obtenerRolActivo(ROL_USUARIO);
        OffsetDateTime ahora = OffsetDateTime.now();
        String telefono = limpiarTextoOpcional(request.getTelefono());

        Usuario usuario = crearUsuario(
                rol,
                request.getNombre(),
                request.getApellido(),
                emailNormalizado,
                request.getPassword(),
                telefono,
                ahora
        );

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return crearLoginResponse(UsuarioPrincipal.desdeUsuario(usuarioGuardado));
    }

    @Transactional
    public LoginResponseDTO registrarPublicador(RegistroPublicadorRequestDTO request) {
        if (request == null) {
            throw new RegistroInvalidoException("El registro no puede estar vacio.");
        }

        String emailNormalizado = normalizarEmail(request.getEmail());
        validarEmailDisponible(emailNormalizado);
        validarPassword(request.getPassword(), request.getConfirmarPassword());

        Rol rol = obtenerRolActivo(ROL_PUBLICADOR);
        Ciudad ciudadPrincipal = ciudadRepository.findByIdAndActivaTrue(request.getCiudadPrincipalId())
                .orElseThrow(() -> new RegistroInvalidoException("La ciudad principal seleccionada no existe."));

        String tipoPublicador = normalizarTipoPublicador(request.getTipoPublicador());
        String whatsapp = limpiarTextoRequerido(request.getWhatsapp(), "El WhatsApp es obligatorio.");
        String whatsappNormalizado = normalizarTelefono(whatsapp, "El WhatsApp debe contener al menos un digito.");
        OffsetDateTime ahora = OffsetDateTime.now();

        Usuario usuario = crearUsuario(
                rol,
                request.getNombre(),
                request.getApellido(),
                emailNormalizado,
                request.getPassword(),
                null,
                ahora
        );

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        PerfilPublicador perfil = crearPerfilPublicador(
                usuarioGuardado,
                ciudadPrincipal,
                tipoPublicador,
                request,
                whatsapp,
                whatsappNormalizado,
                ahora
        );
        perfilPublicadorRepository.save(perfil);

        return crearLoginResponse(UsuarioPrincipal.desdeUsuario(usuarioGuardado));
    }

    @Transactional(readOnly = true)
    public UsuarioActualDTO obtenerUsuarioActual(Long userId) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Usuario usuario = usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));

        return UsuarioActualDTO.desdeUsuario(usuario);
    }

    private UsuarioPrincipal obtenerUsuarioPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
            return usuarioPrincipal;
        }

        throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
    }

    private LoginResponseDTO crearLoginResponse(UsuarioPrincipal usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);

        return new LoginResponseDTO(
                "Bearer",
                accessToken,
                jwtService.getExpiresIn(),
                AuthUsuarioDTO.desdePrincipal(usuario)
        );
    }

    private Usuario crearUsuario(
            Rol rol,
            String nombre,
            String apellido,
            String emailNormalizado,
            String password,
            String telefono,
            OffsetDateTime ahora
    ) {
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre(limpiarTextoRequerido(nombre, "El nombre es obligatorio."));
        usuario.setApellido(limpiarTextoRequerido(apellido, "El apellido es obligatorio."));
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setTelefono(telefono);
        usuario.setTelefonoNormalizado(normalizarTelefonoOpcional(telefono));
        usuario.setTelefonoVerificado(Boolean.FALSE);
        usuario.setActivo(Boolean.TRUE);
        usuario.setEmailVerificado(Boolean.TRUE);
        usuario.setCreatedAt(ahora);
        usuario.setUpdatedAt(ahora);
        return usuario;
    }

    private PerfilPublicador crearPerfilPublicador(
            Usuario usuario,
            Ciudad ciudadPrincipal,
            String tipoPublicador,
            RegistroPublicadorRequestDTO request,
            String whatsapp,
            String whatsappNormalizado,
            OffsetDateTime ahora
    ) {
        String telefonoContacto = limpiarTextoOpcional(request.getTelefonoContacto());

        PerfilPublicador perfil = new PerfilPublicador();
        perfil.setUsuario(usuario);
        perfil.setNombre(limpiarTextoRequerido(request.getNombrePublico(), "El nombre publico es obligatorio."));
        perfil.setTipoPublicador(tipoPublicador);
        perfil.setEstado(ESTADO_PERFIL_PENDIENTE_REVISION);
        perfil.setCiudadPrincipal(ciudadPrincipal);
        perfil.setDescripcion(limpiarTextoOpcional(request.getDescripcion()));
        perfil.setEmailContacto(normalizarEmailOpcional(request.getEmailContacto()));
        perfil.setTelefonoContacto(telefonoContacto);
        perfil.setTelefonoContactoNormalizado(normalizarTelefonoOpcional(telefonoContacto));
        perfil.setWhatsapp(whatsapp);
        perfil.setWhatsappNormalizado(whatsappNormalizado);
        perfil.setInstagram(limpiarTextoOpcional(request.getInstagram()));
        perfil.setActivo(Boolean.TRUE);
        perfil.setVerificado(Boolean.FALSE);
        perfil.setCreatedAt(ahora);
        perfil.setUpdatedAt(ahora);
        return perfil;
    }

    private void validarEmailDisponible(String emailNormalizado) {
        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            throw new RegistroInvalidoException("El email es obligatorio.");
        }

        if (usuarioRepository.existsByEmailNormalizado(emailNormalizado)) {
            throw new EmailYaRegistradoException("El email ya esta registrado.");
        }
    }

    private void validarPassword(String password, String confirmarPassword) {
        if (password == null || password.isBlank()) {
            throw new RegistroInvalidoException("La password es obligatoria.");
        }

        if (confirmarPassword == null || confirmarPassword.isBlank()) {
            throw new RegistroInvalidoException("La confirmacion de password es obligatoria.");
        }

        if (!password.equals(confirmarPassword)) {
            throw new RegistroInvalidoException("La password y su confirmacion no coinciden.");
        }

        boolean tieneLongitudMinima = password.length() >= 8;
        boolean tieneLetra = password.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = password.chars().anyMatch(Character::isDigit);

        if (!tieneLongitudMinima || !tieneLetra || !tieneNumero) {
            throw new RegistroInvalidoException("La password no cumple los requisitos minimos.");
        }
    }

    private Rol obtenerRolActivo(String nombreRol) {
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new ConfiguracionSistemaInvalidaException("Configuracion de roles invalida."));

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new ConfiguracionSistemaInvalidaException("Configuracion de roles invalida.");
        }

        return rol;
    }

    private String normalizarTipoPublicador(String tipoPublicador) {
        String tipoNormalizado = limpiarTextoRequerido(
                tipoPublicador,
                "El tipo de publicador es obligatorio."
        ).toUpperCase(Locale.ROOT);

        if (!TIPOS_PUBLICADOR_VALIDOS.contains(tipoNormalizado)) {
            throw new RegistroInvalidoException("El tipo de publicador informado no es valido.");
        }

        return tipoNormalizado;
    }

    private String normalizarTelefonoOpcional(String telefono) {
        if (telefono == null) {
            return null;
        }

        return normalizarTelefono(telefono, "El telefono debe contener al menos un digito.");
    }

    private String normalizarTelefono(String telefono, String mensajeSinDigitos) {
        String telefonoNormalizado = telefono.replaceAll("[^0-9]", "");

        if (telefonoNormalizado.isBlank()) {
            throw new RegistroInvalidoException(mensajeSinDigitos);
        }

        if (telefonoNormalizado.length() > MAX_LONGITUD_TELEFONO_NORMALIZADO) {
            throw new RegistroInvalidoException("El telefono normalizado no puede superar los 30 digitos.");
        }

        return telefonoNormalizado;
    }

    private String limpiarTextoRequerido(String texto, String mensajeSiFalta) {
        String textoLimpio = limpiarTextoOpcional(texto);

        if (textoLimpio == null) {
            throw new RegistroInvalidoException(mensajeSiFalta);
        }

        return textoLimpio;
    }

    private String limpiarTextoOpcional(String texto) {
        if (texto == null) {
            return null;
        }

        String textoLimpio = texto.trim();

        if (textoLimpio.isEmpty()) {
            return null;
        }

        return textoLimpio;
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarEmailOpcional(String email) {
        String emailLimpio = limpiarTextoOpcional(email);

        if (emailLimpio == null) {
            return null;
        }

        return emailLimpio.toLowerCase(Locale.ROOT);
    }
}
