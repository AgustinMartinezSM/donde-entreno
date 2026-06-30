package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.LoginRequestDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD_FICTICIO = "password-ficticio";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

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

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenReturn(authentication);
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
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
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

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenReturn(authentication);

        CredencialesInvalidasException exception = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequestDTO("admin@dondeentreno.com", PASSWORD_FICTICIO))
        );

        assertEquals("Email o password invalidos.", exception.getMessage());
        assertInstanceOf(CredencialesInvalidasException.class, exception);
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
}
