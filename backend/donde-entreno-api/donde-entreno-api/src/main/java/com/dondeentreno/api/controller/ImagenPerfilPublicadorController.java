package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ImagenPublicadorDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.ImagenPublicadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Logo y portada del perfil publicador.
 *
 * Mismo circuito de moderación que las imágenes de actividad: suben
 * PENDIENTE al bucket privado y solo se ven en público cuando un admin
 * las aprueba. Cuelgan del perfil, no de una actividad.
 *
 * La ruta cae bajo /api/publicador/**, que SecurityConfig ya restringe
 * al rol PUBLICADOR.
 */
@RestController
@RequestMapping("/api/publicador/perfil/imagenes")
public class ImagenPerfilPublicadorController {

    private final ImagenPublicadorService imagenPublicadorService;

    public ImagenPerfilPublicadorController(ImagenPublicadorService imagenPublicadorService) {
        this.imagenPublicadorService = imagenPublicadorService;
    }

    /**
     * @param tipo LOGO o PORTADA.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenPublicadorDTO> subirImagen(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("tipo") String tipo
    ) {
        ImagenPublicadorDTO creada = imagenPublicadorService.subirImagenDePerfil(
                extraerUserId(jwt),
                archivo,
                tipo
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public List<ImagenPublicadorDTO> listarMias(@AuthenticationPrincipal Jwt jwt) {
        return imagenPublicadorService.listarMiasDePerfil(extraerUserId(jwt));
    }

    @DeleteMapping("/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long imagenId
    ) {
        imagenPublicadorService.eliminarMiaDePerfil(extraerUserId(jwt), imagenId);

        return ResponseEntity.noContent().build();
    }

    private Long extraerUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CredencialesInvalidasException("No autenticado.");
        }

        Object userId = jwt.getClaim("userId");

        if (userId instanceof Number numero) {
            return numero.longValue();
        }

        throw new CredencialesInvalidasException("Token sin identificador de usuario.");
    }
}
