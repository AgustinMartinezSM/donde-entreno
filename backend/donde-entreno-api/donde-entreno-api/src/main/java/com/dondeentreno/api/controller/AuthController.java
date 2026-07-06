package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.LoginRequestDTO;
import com.dondeentreno.api.dto.LoginResponseDTO;
import com.dondeentreno.api.dto.RegistroPublicadorRequestDTO;
import com.dondeentreno.api.dto.RegistroUsuarioRequestDTO;
import com.dondeentreno.api.dto.UsuarioActualDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos de autenticacion.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro/usuario")
    public ResponseEntity<LoginResponseDTO> registrarUsuario(
            @Valid @RequestBody RegistroUsuarioRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarUsuario(request));
    }

    @PostMapping("/registro/publicador")
    public ResponseEntity<LoginResponseDTO> registrarPublicador(
            @Valid @RequestBody RegistroPublicadorRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarPublicador(request));
    }

    @GetMapping("/me")
    public UsuarioActualDTO obtenerUsuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return authService.obtenerUsuarioActual(extraerUserId(jwt));
    }

    private Long extraerUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Object userId = jwt.getClaim("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String texto) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException exception) {
                throw new CredencialesInvalidasException("No autenticado.");
            }
        }

        throw new CredencialesInvalidasException("No autenticado.");
    }
}
