package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ActualizarImagenRequestDTO;
import com.dondeentreno.api.dto.ImagenPublicadorDTO;
import com.dondeentreno.api.dto.OrdenarImagenesRequestDTO;
import com.dondeentreno.api.exception.CredencialesInvalidasException;
import com.dondeentreno.api.service.ImagenPublicadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Subida y gestión de imágenes de actividades del publicador.
 * Las imágenes suben PENDIENTE y recién se ven en público cuando
 * un admin las aprueba.
 */
@RestController
@RequestMapping("/api/publicador/actividades/{actividadId}/imagenes")
public class ImagenPublicadorController {

    private final ImagenPublicadorService imagenPublicadorService;

    public ImagenPublicadorController(ImagenPublicadorService imagenPublicadorService) {
        this.imagenPublicadorService = imagenPublicadorService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenPublicadorDTO> subirImagen(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("tipo") String tipo
    ) {
        ImagenPublicadorDTO respuesta = imagenPublicadorService.subirImagen(
                extraerUserId(jwt),
                actividadId,
                archivo,
                tipo
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public List<ImagenPublicadorDTO> listarImagenes(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId
    ) {
        return imagenPublicadorService.listarMias(extraerUserId(jwt), actividadId);
    }

    /**
     * Orden manual de la galería: la lista trae TODAS las fotos GALERIA
     * activas de la actividad en el orden deseado. 204: el frontend
     * relee el listado, que ya viene ordenado.
     */
    @PutMapping("/orden")
    public ResponseEntity<Void> ordenarGaleria(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @Valid @RequestBody OrdenarImagenesRequestDTO request
    ) {
        imagenPublicadorService.ordenarGaleria(
                extraerUserId(jwt),
                actividadId,
                request.getImagenIds()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Promueve una foto aprobada de la galería a PRINCIPAL (swap con la
     * vigente, sin re-moderación).
     */
    @PutMapping("/{imagenId}/principal")
    public ResponseEntity<Void> elegirPrincipal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @PathVariable Long imagenId
    ) {
        imagenPublicadorService.elegirPrincipal(extraerUserId(jwt), actividadId, imagenId);

        return ResponseEntity.noContent().build();
    }

    /** Título y descripción (alt/epígrafe públicos) de una imagen propia. */
    @PatchMapping("/{imagenId}")
    public ImagenPublicadorDTO actualizarImagen(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @PathVariable Long imagenId,
            @Valid @RequestBody ActualizarImagenRequestDTO request
    ) {
        return imagenPublicadorService.actualizarTexto(
                extraerUserId(jwt),
                actividadId,
                imagenId,
                request.getTitulo(),
                request.getDescripcion()
        );
    }

    @DeleteMapping("/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long actividadId,
            @PathVariable Long imagenId
    ) {
        imagenPublicadorService.eliminarMia(extraerUserId(jwt), actividadId, imagenId);

        return ResponseEntity.noContent().build();
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
