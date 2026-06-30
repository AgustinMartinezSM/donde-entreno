package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.AuthUsuarioDTO;
import com.dondeentreno.api.dto.LoginRequestDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.security.JwtService;
import com.dondeentreno.api.security.UsuarioPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Servicio de autenticacion por email/password.
 */
@Service
public class AuthService {

    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Email o password invalidos.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

    private UsuarioPrincipal obtenerUsuarioPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
            return usuarioPrincipal;
        }

        throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
