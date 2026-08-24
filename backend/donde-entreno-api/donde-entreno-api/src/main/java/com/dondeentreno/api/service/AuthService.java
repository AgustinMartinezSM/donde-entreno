package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.AuthUsuarioDTO;
import com.dondeentreno.api.dto.CambiarPasswordRequestDTO;
import com.dondeentreno.api.dto.LoginRequestDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.dto.RegistroPublicadorRequestDTO;
import com.dondeentreno.api.dto.RegistroUsuarioRequestDTO;
import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CambioPasswordInvalidoException;
import com.dondeentreno.api.exception.ConfiguracionSistemaInvalidaException;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.EmailYaRegistradoException;
import com.dondeentreno.api.exception.LimiteConsultasExcedidoException;
import com.dondeentreno.api.exception.RecursoNoEncontradoException;
import com.dondeentreno.api.exception.RegistroInvalidoException;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.security.JwtService;
import com.dondeentreno.api.security.LimitadorCambioPassword;
import com.dondeentreno.api.security.UsuarioPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
    private final RefreshTokenService refreshTokenService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilPublicadorRepository perfilPublicadorRepository;
    private final CiudadRepository ciudadRepository;
    private final PasswordEncoder passwordEncoder;
    private final LimitadorCambioPassword limitadorCambioPassword;
    private final PerfilPublicadorSlugService perfilPublicadorSlugService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilPublicadorRepository perfilPublicadorRepository,
            CiudadRepository ciudadRepository,
            PasswordEncoder passwordEncoder,
            LimitadorCambioPassword limitadorCambioPassword,
            PerfilPublicadorSlugService perfilPublicadorSlugService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilPublicadorRepository = perfilPublicadorRepository;
        this.ciudadRepository = ciudadRepository;
        this.passwordEncoder = passwordEncoder;
        this.limitadorCambioPassword = limitadorCambioPassword;
        this.perfilPublicadorSlugService = perfilPublicadorSlugService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String emailNormalizado = normalizarEmail(request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailNormalizado, request.getPassword())
            );

            UsuarioPrincipal usuario = obtenerUsuarioPrincipal(authentication);
            /* Higiene sin scheduler: el login barre los vencidos viejos del usuario. */
            refreshTokenService.limpiarVencidosDe(usuario.getId());

            return crearLoginResponse(usuario);
        } catch (AuthenticationException exception) {
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
        }
    }

    /**
     * Rota un refresh token valido y devuelve una sesion nueva completa
     * (access + refresh). El usuario se recarga con el filtro de
     * activo/deleted: desactivar una cuenta mata sus refresh tokens en
     * el proximo intento, sin esperar a que expiren.
     */
    public LoginResponseDTO refrescar(String refreshToken) {
        RefreshTokenService.Rotacion rotacion = refreshTokenService.rotar(refreshToken);

        Usuario usuario = usuarioRepository
                .findByIdAndActivoTrueAndDeletedAtIsNull(rotacion.usuarioId())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));

        UsuarioPrincipal principal = UsuarioPrincipal.desdeUsuario(usuario);
        String accessToken = jwtService.generarAccessToken(principal);

        LoginResponseDTO respuesta = new LoginResponseDTO(
                "Bearer",
                accessToken,
                jwtService.getExpiresIn(),
                AuthUsuarioDTO.desdePrincipal(principal)
        );
        respuesta.setRefreshToken(rotacion.token().token());
        respuesta.setRefreshExpiresIn(rotacion.token().expiresInSeconds());
        return respuesta;
    }

    /** Logout real: revoca la familia del refresh token en el servidor. */
    public void cerrarSesion(String refreshToken) {
        refreshTokenService.revocarFamiliaDe(refreshToken);
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

    /**
     * Cambio de password con sesion activa (fase 5a).
     *
     * El orden importa: el freno de fuerza bruta corre antes que nada
     * (un atacante con sesion robada no puede probar passwords), la
     * actual se verifica antes de validar la nueva (el fallo cuenta en
     * el limitador aunque la transaccion haga rollback: vive en
     * memoria), y la revocacion total corre ANTES de emitir la sesion
     * nueva, asi la barrida no la alcanza.
     *
     * Los access tokens ya emitidos en otros dispositivos siguen
     * validos hasta expirar (no hay blacklist de JWT, igual que en el
     * logout): el refresh revocado garantiza que esas sesiones no
     * sobreviven la hora.
     */
    @Transactional
    public LoginResponseDTO cambiarPassword(Long userId, CambiarPasswordRequestDTO request) {
        if (userId == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        if (limitadorCambioPassword.estaBloqueado(userId)) {
            throw new LimiteConsultasExcedidoException(
                    "Demasiados intentos. Proba de nuevo en unos minutos."
            );
        }

        Usuario usuario = usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            limitadorCambioPassword.registrarFallo(userId);
            throw new CambioPasswordInvalidoException("La password actual no es correcta.");
        }

        validarPassword(request.getPasswordNueva(), request.getConfirmarPassword());

        if (request.getPasswordNueva().equals(request.getPasswordActual())) {
            throw new CambioPasswordInvalidoException(
                    "La password nueva no puede ser igual a la actual."
            );
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setUpdatedAt(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        int tokensRevocados = refreshTokenService.revocarTodasDe(userId);
        limitadorCambioPassword.registrarExito(userId);

        /* Solo metadata, nunca passwords: la linea tiene que poder greparse. */
        log.info(
                "Auth: PASSWORD_CAMBIADO usuarioId={} tokensRevocados={}",
                userId,
                tokensRevocados
        );

        return crearLoginResponse(UsuarioPrincipal.desdeUsuario(usuario));
    }

    private UsuarioPrincipal obtenerUsuarioPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
            return usuarioPrincipal;
        }

        throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
    }

    private LoginResponseDTO crearLoginResponse(UsuarioPrincipal usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        RefreshTokenService.TokenEmitido refresh =
                refreshTokenService.emitirParaSesionNueva(usuario.getId());

        LoginResponseDTO respuesta = new LoginResponseDTO(
                "Bearer",
                accessToken,
                jwtService.getExpiresIn(),
                AuthUsuarioDTO.desdePrincipal(usuario)
        );
        respuesta.setRefreshToken(refresh.token());
        respuesta.setRefreshExpiresIn(refresh.expiresInSeconds());
        return respuesta;
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
        perfil.setSlug(perfilPublicadorSlugService.generarSlugUnico(perfil.getNombre()));
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
