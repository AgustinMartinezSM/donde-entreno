package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.LoginRequestDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.dto.RegistroPublicadorRequestDTO;
import com.dondeentreno.api.dto.RegistroUsuarioRequestDTO;
import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.entity.Ciudad;
import com.dondeentreno.api.entity.PerfilPublicador;
import com.dondeentreno.api.entity.Rol;
import com.dondeentreno.api.entity.Usuario;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.exception.EmailYaRegistradoException;
import com.dondeentreno.api.exception.RegistroInvalidoException;
import com.dondeentreno.api.repository.CiudadRepository;
import com.dondeentreno.api.repository.PerfilPublicadorRepository;
import com.dondeentreno.api.repository.RolRepository;
import com.dondeentreno.api.repository.UsuarioRepository;
import com.dondeentreno.api.security.JwtService;
import com.dondeentreno.api.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD_FICTICIO = "password-ficticio";
    private static final String PASSWORD_SEGURA = "Password1";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PerfilPublicadorRepository perfilPublicadorRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginExitosoNormalizaEmailAutenticaGeneraTokenYDevuelveUsuario() {
        UsuarioPrincipal principal = crearPrincipal("admin@dondeentreno.com", "SUPER_ADMIN");
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtService.generarAccessToken(principal)).thenReturn("jwt-ficticio");
        when(jwtService.getExpiresIn()).thenReturn(3600L);

        LoginResponseDTO response = authService.login(new LoginRequestDTO(
                "  ADMIN@DONDEENTRENO.COM  ",
                PASSWORD_FICTICIO
        ));

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        Authentication autenticacionEnviada = authenticationCaptor.getValue();
        assertEquals("admin@dondeentreno.com", autenticacionEnviada.getPrincipal());
        assertEquals(PASSWORD_FICTICIO, autenticacionEnviada.getCredentials());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("jwt-ficticio", response.getAccessToken());
        assertEquals(3600L, response.getExpiresIn());
        assertNotNull(response.getUsuario());
        assertEquals(1L, response.getUsuario().getId());
        assertEquals("admin@dondeentreno.com", response.getUsuario().getEmail());
        assertEquals("Admin", response.getUsuario().getNombre());
        assertEquals("SUPER_ADMIN", response.getUsuario().getRol());
    }

    @Test
    void credencialesInvalidasDevuelveExcepcionGenerica() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        CredencialesInvalidasException exception = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequestDTO("admin@dondeentreno.com", PASSWORD_FICTICIO))
        );

        assertEquals("Email o password invalidos.", exception.getMessage());
    }

    @Test
    void principalInesperadoDevuelveExcepcionGenerica() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin@dondeentreno.com",
                null,
                java.util.List.of()
        );

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        CredencialesInvalidasException exception = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequestDTO("admin@dondeentreno.com", PASSWORD_FICTICIO))
        );

        assertEquals("Email o password invalidos.", exception.getMessage());
        assertInstanceOf(CredencialesInvalidasException.class, exception);
    }

    @Test
    void registrarUsuarioValidoCreaUsuarioHasheaPasswordYDevuelveToken() {
        RegistroUsuarioRequestDTO request = registroUsuarioRequest();
        Rol rol = rol("USUARIO");

        when(usuarioRepository.existsByEmailNormalizado("usuario@ejemplo.com")).thenReturn(false);
        when(rolRepository.findByNombre("USUARIO")).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode(PASSWORD_SEGURA)).thenReturn("$2a$10$hash-ficticio-no-real");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });
        when(jwtService.generarAccessToken(any(UsuarioPrincipal.class))).thenReturn("jwt-usuario");
        when(jwtService.getExpiresIn()).thenReturn(3600L);

        LoginResponseDTO response = authService.registrarUsuario(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioGuardado = usuarioCaptor.getValue();

        assertSame(rol, usuarioGuardado.getRol());
        assertEquals("Usuario", usuarioGuardado.getNombre());
        assertEquals("Prueba", usuarioGuardado.getApellido());
        assertEquals("usuario@ejemplo.com", usuarioGuardado.getEmail());
        assertNotEquals(PASSWORD_SEGURA, usuarioGuardado.getPasswordHash());
        assertEquals("$2a$10$hash-ficticio-no-real", usuarioGuardado.getPasswordHash());
        assertEquals("+54 223 555 1234", usuarioGuardado.getTelefono());
        assertEquals("542235551234", usuarioGuardado.getTelefonoNormalizado());
        assertFalse(usuarioGuardado.getTelefonoVerificado());
        assertTrue(usuarioGuardado.getActivo());
        assertTrue(usuarioGuardado.getEmailVerificado());
        assertNotNull(usuarioGuardado.getCreatedAt());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("jwt-usuario", response.getAccessToken());
        assertEquals("USUARIO", response.getUsuario().getRol());
        assertEquals("usuario@ejemplo.com", response.getUsuario().getEmail());
    }

    @Test
    void registrarUsuarioConEmailDuplicadoLanzaErrorYNoGuarda() {
        RegistroUsuarioRequestDTO request = registroUsuarioRequest();
        when(usuarioRepository.existsByEmailNormalizado("usuario@ejemplo.com")).thenReturn(true);

        EmailYaRegistradoException exception = assertThrows(
                EmailYaRegistradoException.class,
                () -> authService.registrarUsuario(request)
        );

        assertEquals("El email ya esta registrado.", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarUsuarioConPasswordDebilLanzaErrorYNoGuarda() {
        RegistroUsuarioRequestDTO request = registroUsuarioRequest();
        request.setPassword("abcdefg");
        request.setConfirmarPassword("abcdefg");
        when(usuarioRepository.existsByEmailNormalizado("usuario@ejemplo.com")).thenReturn(false);

        RegistroInvalidoException exception = assertThrows(
                RegistroInvalidoException.class,
                () -> authService.registrarUsuario(request)
        );

        assertEquals("La password no cumple los requisitos minimos.", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarUsuarioConPasswordYConfirmacionDistintasLanzaErrorYNoGuarda() {
        RegistroUsuarioRequestDTO request = registroUsuarioRequest();
        request.setConfirmarPassword("OtraPassword1");
        when(usuarioRepository.existsByEmailNormalizado("usuario@ejemplo.com")).thenReturn(false);

        RegistroInvalidoException exception = assertThrows(
                RegistroInvalidoException.class,
                () -> authService.registrarUsuario(request)
        );

        assertEquals("La password y su confirmacion no coinciden.", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarPublicadorValidoCreaUsuarioPerfilYDevuelveToken() {
        RegistroPublicadorRequestDTO request = registroPublicadorRequest();
        Rol rol = rol("PUBLICADOR");
        Ciudad ciudad = ciudad(3L, "Mar del Plata");

        when(usuarioRepository.existsByEmailNormalizado("publicador@ejemplo.com")).thenReturn(false);
        when(rolRepository.findByNombre("PUBLICADOR")).thenReturn(Optional.of(rol));
        when(ciudadRepository.findByIdAndActivaTrue(3L)).thenReturn(Optional.of(ciudad));
        when(passwordEncoder.encode(PASSWORD_SEGURA)).thenReturn("$2a$10$hash-publicador-ficticio");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(20L);
            return usuario;
        });
        when(perfilPublicadorRepository.save(any(PerfilPublicador.class))).thenAnswer(invocation -> {
            PerfilPublicador perfil = invocation.getArgument(0);
            perfil.setId(30L);
            return perfil;
        });
        when(jwtService.generarAccessToken(any(UsuarioPrincipal.class))).thenReturn("jwt-publicador");
        when(jwtService.getExpiresIn()).thenReturn(3600L);

        LoginResponseDTO response = authService.registrarPublicador(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<PerfilPublicador> perfilCaptor = ArgumentCaptor.forClass(PerfilPublicador.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(perfilPublicadorRepository).save(perfilCaptor.capture());

        Usuario usuarioGuardado = usuarioCaptor.getValue();
        PerfilPublicador perfilGuardado = perfilCaptor.getValue();

        assertSame(rol, usuarioGuardado.getRol());
        assertEquals("publicador@ejemplo.com", usuarioGuardado.getEmail());
        assertTrue(usuarioGuardado.getActivo());
        assertTrue(usuarioGuardado.getEmailVerificado());
        assertFalse(usuarioGuardado.getTelefonoVerificado());
        assertSame(usuarioGuardado, perfilGuardado.getUsuario());
        assertEquals("Perfil Publicador", perfilGuardado.getNombre());
        assertEquals("PROFESOR_INDEPENDIENTE", perfilGuardado.getTipoPublicador());
        assertEquals("PENDIENTE_REVISION", perfilGuardado.getEstado());
        assertSame(ciudad, perfilGuardado.getCiudadPrincipal());
        assertEquals("+54 223 555 9999", perfilGuardado.getWhatsapp());
        assertEquals("542235559999", perfilGuardado.getWhatsappNormalizado());
        assertEquals("+54 223 555 8888", perfilGuardado.getTelefonoContacto());
        assertEquals("542235558888", perfilGuardado.getTelefonoContactoNormalizado());
        assertEquals("contacto@ejemplo.com", perfilGuardado.getEmailContacto());
        assertTrue(perfilGuardado.getActivo());
        assertFalse(perfilGuardado.getVerificado());
        assertEquals("jwt-publicador", response.getAccessToken());
        assertEquals("PUBLICADOR", response.getUsuario().getRol());
    }

    @Test
    void registrarPublicadorSinWhatsappLanzaError() {
        RegistroPublicadorRequestDTO request = registroPublicadorRequest();
        request.setWhatsapp("   ");

        when(usuarioRepository.existsByEmailNormalizado("publicador@ejemplo.com")).thenReturn(false);
        when(rolRepository.findByNombre("PUBLICADOR")).thenReturn(Optional.of(rol("PUBLICADOR")));
        when(ciudadRepository.findByIdAndActivaTrue(3L)).thenReturn(Optional.of(ciudad(3L, "Mar del Plata")));

        RegistroInvalidoException exception = assertThrows(
                RegistroInvalidoException.class,
                () -> authService.registrarPublicador(request)
        );

        assertEquals("El WhatsApp es obligatorio.", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(perfilPublicadorRepository, never()).save(any());
    }

    @Test
    void registrarPublicadorConCiudadInexistenteLanzaError() {
        RegistroPublicadorRequestDTO request = registroPublicadorRequest();

        when(usuarioRepository.existsByEmailNormalizado("publicador@ejemplo.com")).thenReturn(false);
        when(rolRepository.findByNombre("PUBLICADOR")).thenReturn(Optional.of(rol("PUBLICADOR")));
        when(ciudadRepository.findByIdAndActivaTrue(3L)).thenReturn(Optional.empty());

        RegistroInvalidoException exception = assertThrows(
                RegistroInvalidoException.class,
                () -> authService.registrarPublicador(request)
        );

        assertEquals("La ciudad principal seleccionada no existe.", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(perfilPublicadorRepository, never()).save(any());
    }

    @Test
    void obtenerUsuarioActualConUserIdValidoDevuelveDatosSinHash() {
        Usuario usuario = usuario(40L, "usuario@ejemplo.com", "USUARIO");
        when(usuarioRepository.findByIdAndActivoTrueAndDeletedAtIsNull(40L)).thenReturn(Optional.of(usuario));

        UsuarioActualDTO response = authService.obtenerUsuarioActual(40L);

        assertEquals(40L, response.getId());
        assertEquals("Usuario", response.getNombre());
        assertEquals("usuario@ejemplo.com", response.getEmail());
        assertEquals("USUARIO", response.getRol());
        assertEquals("+54 223 555 1234", response.getTelefono());
        assertTrue(response.getActivo());
        assertTrue(response.getEmailVerificado());
    }

    private RegistroUsuarioRequestDTO registroUsuarioRequest() {
        RegistroUsuarioRequestDTO request = new RegistroUsuarioRequestDTO();
        request.setNombre(" Usuario ");
        request.setApellido(" Prueba ");
        request.setEmail("  USUARIO@EJEMPLO.COM ");
        request.setPassword(PASSWORD_SEGURA);
        request.setConfirmarPassword(PASSWORD_SEGURA);
        request.setTelefono("+54 223 555 1234");
        return request;
    }

    private RegistroPublicadorRequestDTO registroPublicadorRequest() {
        RegistroPublicadorRequestDTO request = new RegistroPublicadorRequestDTO();
        request.setNombre(" Publicador ");
        request.setApellido(" Prueba ");
        request.setEmail(" PUBLICADOR@EJEMPLO.COM ");
        request.setPassword(PASSWORD_SEGURA);
        request.setConfirmarPassword(PASSWORD_SEGURA);
        request.setWhatsapp("+54 223 555 9999");
        request.setTipoPublicador("PROFESOR_INDEPENDIENTE");
        request.setNombrePublico(" Perfil Publicador ");
        request.setCiudadPrincipalId(3L);
        request.setDescripcion(" Descripcion del perfil ");
        request.setInstagram(" @perfil ");
        request.setEmailContacto(" CONTACTO@EJEMPLO.COM ");
        request.setTelefonoContacto("+54 223 555 8888");
        return request;
    }

    private UsuarioPrincipal crearPrincipal(String email, String rol) {
        return new UsuarioPrincipal(
                1L,
                email,
                "Admin",
                null,
                rol,
                "$2a$10$hash-ficticio-no-real",
                true
        );
    }

    private Usuario usuario(Long id, String email, String nombreRol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario");
        usuario.setApellido("Prueba");
        usuario.setEmail(email);
        usuario.setPasswordHash("$2a$10$hash-ficticio-no-real");
        usuario.setTelefono("+54 223 555 1234");
        usuario.setActivo(Boolean.TRUE);
        usuario.setEmailVerificado(Boolean.TRUE);
        usuario.setTelefonoVerificado(Boolean.FALSE);
        usuario.setRol(rol(nombreRol));
        return usuario;
    }

    private Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre(nombre);
        rol.setActivo(Boolean.TRUE);
        return rol;
    }

    private Ciudad ciudad(Long id, String nombre) {
        Ciudad ciudad = new Ciudad();
        ciudad.setId(id);
        ciudad.setNombre(nombre);
        ciudad.setProvincia("Buenos Aires");
        ciudad.setPais("Argentina");
        ciudad.setActiva(Boolean.TRUE);
        return ciudad;
    }
}
