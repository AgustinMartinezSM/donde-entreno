package com.dondeentreno.api.controller;

import com.dondeentreno.api.dto.ImagenAdminDTO;
import com.dondeentreno.api.dto.PaginaResponseDTO;
import com.dondeentreno.api.dto.RechazarImagenRequestDTO;
import com.dondeentreno.api.service.ImagenAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cola de moderación de imágenes para el panel admin.
 */
@RestController
@RequestMapping("/api/admin/imagenes")
public class ImagenAdminController {

    private final ImagenAdminService imagenAdminService;

    public ImagenAdminController(ImagenAdminService imagenAdminService) {
        this.imagenAdminService = imagenAdminService;
    }

    @GetMapping
    public PaginaResponseDTO<ImagenAdminDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return imagenAdminService.listar(estado, page, size);
    }

    @PostMapping("/{id}/aprobar")
    public ImagenAdminDTO aprobar(@PathVariable Long id) {
        return imagenAdminService.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    public ImagenAdminDTO rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazarImagenRequestDTO request
    ) {
        return imagenAdminService.rechazar(id, request.getMotivo());
    }
}
